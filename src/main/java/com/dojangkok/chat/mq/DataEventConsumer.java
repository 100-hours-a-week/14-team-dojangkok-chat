package com.dojangkok.chat.mq;

import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.event.DataEventDto;
import com.dojangkok.chat.repository.ChatRoomRepository;
import com.dojangkok.chat.service.PropertyCacheService;
import com.dojangkok.chat.service.UserProfileCacheService;
import com.dojangkok.chat.mq.config.RabbitMQChatConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataEventConsumer {

    private final UserProfileCacheService userProfileCacheService;
    private final PropertyCacheService propertyCacheService;
    private final ChatRoomRepository chatRoomRepository;

    @RabbitListener(queues = RabbitMQChatConfig.DATA_EVENTS_QUEUE)
    public void handleDataEvent(DataEventDto event) {
        log.info("데이터 이벤트 수신: type={}", event.getType());

        try {
            switch (event.getType()) {
                case "USER_UPDATED" -> handleUserUpdated(event);
                case "USER_DELETED" -> handleUserDeleted(event);
                case "PROPERTY_UPDATED" -> handlePropertyUpdated(event);
                case "PROPERTY_DELETED" -> handlePropertyDeleted(event);
                default -> log.warn("알 수 없는 이벤트 타입: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("데이터 이벤트 처리 실패: type={}", event.getType(), e);
        }
    }

    private void handleUserUpdated(DataEventDto event) {
        String userId = event.getUserId();

        // 1. Redis 캐시 삭제
        userProfileCacheService.evict(userId);

        // 2. MongoDB 스냅샷 업데이트
        List<ChatRoom> rooms = chatRoomRepository.findByParticipantsContaining(userId);
        for (ChatRoom room : rooms) {
            if (room.isStaleEvent(event.getEventTimestamp())) {
                log.warn("out-of-order 이벤트 감지, skip: type=USER_UPDATED, userId={}, roomId={}", userId, room.getRoomId());
                continue;
            }

            List<ChatRoom.ParticipantInfo> updatedProfiles = room.getParticipantProfiles().stream()
                    .map(p -> p.getUserId().equals(userId)
                            ? ChatRoom.ParticipantInfo.builder()
                                .userId(userId)
                                .nickname(event.getNickname())
                                .profileImageUrl(event.getProfileImageUrl())
                                .build()
                            : p)
                    .toList();
            room.updateParticipantProfiles(updatedProfiles);
            room.markEventTimestamp(event.getEventTimestamp());
            chatRoomRepository.save(room);
        }

        log.info("유저 프로필 업데이트 처리 완료: userId={}, 채팅방 {}개", userId, rooms.size());
    }

    private void handleUserDeleted(DataEventDto event) {
        String userId = event.getUserId();

        // 1. Redis 캐시 삭제
        userProfileCacheService.evict(userId);

        // 2. MongoDB 스냅샷에 탈퇴 표시
        List<ChatRoom> rooms = chatRoomRepository.findByParticipantsContaining(userId);
        for (ChatRoom room : rooms) {
            if (room.isStaleEvent(event.getEventTimestamp())) {
                log.warn("out-of-order 이벤트 감지, skip: type=USER_DELETED, userId={}, roomId={}", userId, room.getRoomId());
                continue;
            }

            List<ChatRoom.ParticipantInfo> updatedProfiles = room.getParticipantProfiles().stream()
                    .map(p -> p.getUserId().equals(userId)
                            ? ChatRoom.ParticipantInfo.builder()
                                .userId(userId)
                                .nickname("탈퇴한 사용자")
                                .profileImageUrl(null)
                                .build()
                            : p)
                    .toList();
            room.updateParticipantProfiles(updatedProfiles);
            room.markEventTimestamp(event.getEventTimestamp());
            chatRoomRepository.save(room);
        }

        log.info("유저 탈퇴 처리 완료: userId={}, 채팅방 {}개", userId, rooms.size());
    }

    private void handlePropertyUpdated(DataEventDto event) {
        String propertyId = event.getPropertyId();

        // 1. Redis 캐시 삭제
        propertyCacheService.evict(propertyId);

        // 2. MongoDB 스냅샷 업데이트
        List<ChatRoom> rooms = chatRoomRepository.findByPropertyId(propertyId);
        for (ChatRoom room : rooms) {
            if (room.isStaleEvent(event.getEventTimestamp())) {
                log.warn("out-of-order 이벤트 감지, skip: type=PROPERTY_UPDATED, propertyId={}, roomId={}", propertyId, room.getRoomId());
                continue;
            }

            if (event.getTitle() != null) {
                room.updatePropertySnapshot(event.getTitle(),
                        event.getImageUrl() != null ? event.getImageUrl() : room.getPropertyImageUrl());
            }
            room.markEventTimestamp(event.getEventTimestamp());
            chatRoomRepository.save(room);
        }

        log.info("매물 정보 업데이트 처리 완료: propertyId={}, 채팅방 {}개", propertyId, rooms.size());
    }

    private void handlePropertyDeleted(DataEventDto event) {
        String propertyId = event.getPropertyId();

        // 1. Redis 캐시 삭제
        propertyCacheService.evict(propertyId);

        // 2. MongoDB 스냅샷에 삭제 표시
        List<ChatRoom> rooms = chatRoomRepository.findByPropertyId(propertyId);
        for (ChatRoom room : rooms) {
            if (room.isStaleEvent(event.getEventTimestamp())) {
                log.warn("out-of-order 이벤트 감지, skip: type=PROPERTY_DELETED, propertyId={}, roomId={}", propertyId, room.getRoomId());
                continue;
            }

            room.updatePropertySnapshot("삭제된 매물", null);
            room.markEventTimestamp(event.getEventTimestamp());
            chatRoomRepository.save(room);
        }

        log.info("매물 삭제 처리 완료: propertyId={}, 채팅방 {}개", propertyId, rooms.size());
    }
}
