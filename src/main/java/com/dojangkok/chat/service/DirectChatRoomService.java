package com.dojangkok.chat.service;

import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.common.exception.GeneralException;
import com.dojangkok.chat.domain.ChatReadStatus;
import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.cache.CachedPropertyInfo;
import com.dojangkok.chat.dto.cache.CachedUserProfile;
import com.dojangkok.chat.dto.chatroom.ChatRoomDetailResponse;
import com.dojangkok.chat.dto.chatroom.ChatRoomListResponse;
import com.dojangkok.chat.dto.chatroom.ChatRoomResponse;
import com.dojangkok.chat.dto.chatroom.CreateRoomRequest;
import com.dojangkok.chat.dto.chatroom.CreateRoomResponse;
import com.dojangkok.chat.mapper.ChatRoomMapper;
import com.dojangkok.chat.repository.ChatReadStatusRepository;
import com.dojangkok.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final DirectChatReadStatusService directChatReadStatusService;
    private final ChatReadStatusRepository chatReadStatusRepository;
    private final UserProfileCacheService userProfileCacheService;
    private final PropertyCacheService propertyCacheService;
    private final PropertyCacheLegacyService propertyCacheLegacyService;
    private final ChatRoomMapper chatRoomMapper;

    public CreateRoomResponse getOrCreateDirectChatRoom(String userId, CreateRoomRequest request) {
        if (userId.equals(request.getTargetUserId())) {
            throw new GeneralException(Code.CHAT_SELF_ROOM_NOT_ALLOWED);
        }

        List<String> participants = Stream.of(userId, request.getTargetUserId()).sorted().toList();

        try {
            ChatRoom room = chatRoomRepository
                    .findExistingRoom("DIRECT", participants, request.getPropertyId())
                    .orElseGet(() -> {
                        CachedUserProfile myProfile = userProfileCacheService.getProfile(userId);
                        CachedUserProfile targetProfile = userProfileCacheService.getProfile(request.getTargetUserId());
                        CachedPropertyInfo property = propertyCacheService.getProperty(request.getPropertyId());

                        return chatRoomRepository.save(
                                ChatRoom.builder()
                                        .roomId(UUID.randomUUID().toString())
                                        .type("DIRECT")
                                        .participants(participants)
                                        .propertyId(property.getPropertyId())
                                        .propertyTitle(property.getTitle())
                                        .propertyImageUrl(property.getImageUrl())
                                        .participantProfiles(List.of(
                                                ChatRoom.ParticipantInfo.builder()
                                                        .userId(myProfile.getUserId())
                                                        .nickname(myProfile.getNickname())
                                                        .profileImageUrl(myProfile.getProfileImageUrl())
                                                        .build(),
                                                ChatRoom.ParticipantInfo.builder()
                                                        .userId(targetProfile.getUserId())
                                                        .nickname(targetProfile.getNickname())
                                                        .profileImageUrl(targetProfile.getProfileImageUrl())
                                                        .build()
                                        ))
                                        .createdAt(Instant.now())
                                        .build()
                        );
                    });

            return chatRoomMapper.toCreateResponse(room, userId);
        } catch (DuplicateKeyException e) {
            log.info("채팅방 동시 생성 감지 → 기존 방 반환: userId={}, propertyId={}", userId, request.getPropertyId());
            ChatRoom existingRoom = chatRoomRepository
                    .findExistingRoom("DIRECT", participants, request.getPropertyId())
                    .orElseThrow(() -> new GeneralException(Code.CHAT_ROOM_NOT_FOUND));
            return chatRoomMapper.toCreateResponse(existingRoom, userId);
        }
    }

    public ChatRoomListResponse getMyRooms(String userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByParticipantsContainingAndLastMessageIsNotNullOrderByLastMessage_CreatedAtDesc(userId);

        List<ChatRoomResponse> roomResponses = rooms.stream()
                .map(room -> {
                    long unreadCount = directChatReadStatusService.getUnreadCount(userId, room.getRoomId());
                    return chatRoomMapper.toResponse(room, userId, unreadCount);
                })
                .toList();

        return chatRoomMapper.toListResponse(roomResponses);
    }

    public ChatRoomDetailResponse getRoomDetail(String userId, String roomId, boolean useLegacy) {
        ChatRoom room = getRoomByRoomId(roomId);

        if (!room.getParticipants().contains(userId)) {
            throw new GeneralException(Code.CHAT_ROOM_ACCESS_DENIED);
        }

        // Redis Look-Aside: 최신 프로필/매물 정보 조회
        List<CachedUserProfile> latestProfiles = room.getParticipants().stream()
                .map(userProfileCacheService::getProfile)
                .toList();

        CachedPropertyInfo latestProperty = useLegacy
                ? propertyCacheLegacyService.getProperty(room.getPropertyId())
                : propertyCacheService.getProperty(room.getPropertyId());

        // 메인 서버 응답이 유효한지 확인 (fallback 기본값이 아닌지)
        boolean profilesValid = latestProfiles.stream()
                .noneMatch(p -> "알 수 없음".equals(p.getNickname()));
        boolean propertyValid = latestProperty != null
                && latestProperty.getPropertyId() != null
                && !"알 수 없음".equals(latestProperty.getTitle());

        // 유효한 데이터만 응답에 반영 + Read-Repair, 실패 시 기존 스냅샷 유지
        if (profilesValid) {
            List<ChatRoom.ParticipantInfo> latestParticipantInfos = latestProfiles.stream()
                    .map(profile -> ChatRoom.ParticipantInfo.builder()
                            .userId(profile.getUserId())
                            .nickname(profile.getNickname())
                            .profileImageUrl(profile.getProfileImageUrl())
                            .build())
                    .toList();

            asyncReadRepairProfiles(room, latestParticipantInfos);
            room.updateParticipantProfiles(latestParticipantInfos);
        }

        if (propertyValid) {
            asyncReadRepairProperty(room, latestProperty);
            room.updatePropertySnapshot(latestProperty.getTitle(), latestProperty.getImageUrl());
        }

        // 상대방의 lastReadAt 조회
        String partnerUserId = room.getParticipants().stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);

        Instant partnerLastReadAt = null;
        if (partnerUserId != null) {
            partnerLastReadAt = chatReadStatusRepository.findByRoomIdAndUserId(roomId, partnerUserId)
                    .map(ChatReadStatus::getLastReadAt)
                    .orElse(null);
        }

        // propertyValid면 최신 캐시 데이터, 아니면 스냅샷 기반으로 응답
        CachedPropertyInfo responseProperty = propertyValid ? latestProperty
                : CachedPropertyInfo.builder()
                    .propertyId(room.getPropertyId())
                    .title(room.getPropertyTitle())
                    .imageUrl(room.getPropertyImageUrl())
                    .build();

        return chatRoomMapper.toDetailResponse(room, userId, responseProperty, partnerLastReadAt);
    }

    @Async
    public void asyncReadRepairProfiles(ChatRoom room,
                                        List<ChatRoom.ParticipantInfo> latestParticipantInfos) {
        try {
            if (isProfileChanged(room.getParticipantProfiles(), latestParticipantInfos)) {
                room.updateParticipantProfiles(latestParticipantInfos);
                chatRoomRepository.save(room);
                log.info("Read-Repair: 참여자 프로필 스냅샷 갱신 완료 roomId={}", room.getRoomId());
            }
        } catch (Exception e) {
            log.warn("Read-Repair 실패 (프로필): roomId={} — 다음 조회 시 재시도됩니다. reason={}", room.getRoomId(), e.getMessage());
        }
    }

    @Async
    public void asyncReadRepairProperty(ChatRoom room,
                                        CachedPropertyInfo latestProperty) {
        try {
            if (isPropertyChanged(room, latestProperty)) {
                room.updatePropertySnapshot(latestProperty.getTitle(), latestProperty.getImageUrl());
                chatRoomRepository.save(room);
                log.info("Read-Repair: 매물 정보 스냅샷 갱신 완료 roomId={}", room.getRoomId());
            }
        } catch (Exception e) {
            log.warn("Read-Repair 실패 (매물): roomId={} — 다음 조회 시 재시도됩니다. reason={}", room.getRoomId(), e.getMessage());
        }
    }

    private boolean isProfileChanged(List<ChatRoom.ParticipantInfo> existing,
                                     List<ChatRoom.ParticipantInfo> latest) {
        if (existing == null || latest == null) return true;
        if (existing.size() != latest.size()) return true;

        for (ChatRoom.ParticipantInfo old : existing) {
            ChatRoom.ParticipantInfo updated = latest.stream()
                    .filter(p -> Objects.equals(p.getUserId(), old.getUserId()))
                    .findFirst()
                    .orElse(null);

            if (updated == null) return true;
            if (!Objects.equals(old.getNickname(), updated.getNickname())) return true;
            if (!Objects.equals(old.getProfileImageUrl(), updated.getProfileImageUrl())) return true;
        }
        return false;
    }

    private boolean isPropertyChanged(ChatRoom room, CachedPropertyInfo latest) {
        if (latest == null) return false;
        return !Objects.equals(room.getPropertyTitle(), latest.getTitle())
                || !Objects.equals(room.getPropertyImageUrl(), latest.getImageUrl());
    }

    public ChatRoom getRoomByRoomId(String roomId) {
        return chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new GeneralException(Code.CHAT_ROOM_NOT_FOUND));
    }

    public void updateLastMessage(String roomId, ChatRoom.LastMessage lastMessage) {
        ChatRoom room = getRoomByRoomId(roomId);
        room.updateLastMessage(lastMessage);
        chatRoomRepository.save(room);
    }
}
