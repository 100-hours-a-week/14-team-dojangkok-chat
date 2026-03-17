package com.dojangkok.chat.mapper;

import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.cache.CachedPropertyInfo;
import com.dojangkok.chat.dto.chatroom.ChatRoomDetailResponse;
import com.dojangkok.chat.dto.chatroom.ChatRoomListResponse;
import com.dojangkok.chat.dto.chatroom.ChatRoomResponse;
import com.dojangkok.chat.dto.chatroom.CreateRoomResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ChatRoomMapper {

    public CreateRoomResponse toCreateResponse(ChatRoom room, String userId) {
        return CreateRoomResponse.builder()
                .roomId(room.getRoomId())
                .type(room.getType())
                .partnerInfo(toCreatePartnerInfo(room, userId))
                .property(toCreatePropertyInfo(room))
                .createdAt(room.getCreatedAt())
                .build();
    }

    public ChatRoomResponse toResponse(ChatRoom room, String userId, long unreadCount) {
        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .type(room.getType())
                .partnerInfo(toPartnerInfo(room, userId))
                .property(toPropertyInfo(room))
                .lastMessage(toLastMessage(room, userId))
                .unreadCount(unreadCount)
                .createdAt(room.getCreatedAt())
                .build();
    }

    public ChatRoomDetailResponse toDetailResponse(ChatRoom room, String userId,
                                                   CachedPropertyInfo propertyInfo, Instant partnerLastReadAt) {
        return ChatRoomDetailResponse.builder()
                .roomId(room.getRoomId())
                .type(room.getType())
                .partnerInfo(toDetailPartnerInfo(room, userId))
                .property(toPropertyDetail(propertyInfo))
                .lastMessage(toDetailLastMessage(room, userId))
                .partnerLastReadAt(partnerLastReadAt)
                .createdAt(room.getCreatedAt())
                .build();
    }

    public ChatRoomListResponse toListResponse(List<ChatRoomResponse> roomResponses) {
        return ChatRoomListResponse.builder()
                .totalCount(roomResponses.size())
                .rooms(roomResponses)
                .build();
    }

    // ── 생성용 private 메서드 ──

    private CreateRoomResponse.PartnerInfoDto toCreatePartnerInfo(ChatRoom room, String userId) {
        if (room.getParticipantProfiles() == null) return null;
        return room.getParticipantProfiles().stream()
                .filter(p -> !String.valueOf(p.getUserId()).equals(String.valueOf(userId)))
                .findFirst()
                .map(p -> CreateRoomResponse.PartnerInfoDto.builder()
                        .userId(p.getUserId())
                        .nickname(p.getNickname())
                        .profileImageUrl(p.getProfileImageUrl())
                        .build())
                .orElse(null);
    }

    private CreateRoomResponse.PropertyInfoDto toCreatePropertyInfo(ChatRoom room) {
        if (room.getPropertyId() == null) return null;
        return CreateRoomResponse.PropertyInfoDto.builder()
                .propertyId(room.getPropertyId())
                .title(room.getPropertyTitle())
                .imageUrl(room.getPropertyImageUrl())
                .build();
    }

    // ── 목록용 private 메서드 ──

    private ChatRoomResponse.PartnerInfoDto toPartnerInfo(ChatRoom room, String userId) {
        if (room.getParticipantProfiles() == null) return null;
        return room.getParticipantProfiles().stream()
                .filter(p -> !String.valueOf(p.getUserId()).equals(String.valueOf(userId)))
                .findFirst()
                .map(p -> ChatRoomResponse.PartnerInfoDto.builder()
                        .userId(p.getUserId())
                        .nickname(p.getNickname())
                        .profileImageUrl(p.getProfileImageUrl())
                        .build())
                .orElse(null);
    }

    private ChatRoomResponse.PropertyInfoDto toPropertyInfo(ChatRoom room) {
        if (room.getPropertyId() == null) return null;
        return ChatRoomResponse.PropertyInfoDto.builder()
                .propertyId(room.getPropertyId())
                .title(room.getPropertyTitle())
                .imageUrl(room.getPropertyImageUrl())
                .build();
    }

    private ChatRoomResponse.LastMessageDto toLastMessage(ChatRoom room, String userId) {
        if (room.getLastMessage() == null) return null;
        ChatRoom.LastMessage lastMsg = room.getLastMessage();
        return ChatRoomResponse.LastMessageDto.builder()
                .content(lastMsg.getContent())
                .contentType(lastMsg.getContentType())
                .senderId(lastMsg.getSenderId())
                .mine(String.valueOf(userId).equals(String.valueOf(lastMsg.getSenderId())))
                .createdAt(lastMsg.getCreatedAt())
                .build();
    }

    // ── 상세용 private 메서드 ──

    private ChatRoomDetailResponse.PartnerInfoDto toDetailPartnerInfo(ChatRoom room, String userId) {
        if (room.getParticipantProfiles() == null) return null;
        return room.getParticipantProfiles().stream()
                .filter(p -> !String.valueOf(p.getUserId()).equals(String.valueOf(userId)))
                .findFirst()
                .map(p -> ChatRoomDetailResponse.PartnerInfoDto.builder()
                        .userId(p.getUserId())
                        .nickname(p.getNickname())
                        .profileImageUrl(p.getProfileImageUrl())
                        .build())
                .orElse(null);
    }

    private ChatRoomDetailResponse.PropertyDetailDto toPropertyDetail(CachedPropertyInfo info) {
        if (info == null || info.getPropertyId() == null) return null;
        return ChatRoomDetailResponse.PropertyDetailDto.builder()
                .propertyId(info.getPropertyId())
                .title(info.getTitle())
                .imageUrl(info.getImageUrl())
                .priceMain(info.getPriceMain())
                .priceMonthly(info.getPriceMonthly())
                .rentType(info.getRentType())
                .dealStatus(info.getDealStatus())
                .build();
    }

    private ChatRoomDetailResponse.LastMessageDto toDetailLastMessage(ChatRoom room, String userId) {
        if (room.getLastMessage() == null) return null;
        ChatRoom.LastMessage lastMsg = room.getLastMessage();
        return ChatRoomDetailResponse.LastMessageDto.builder()
                .content(lastMsg.getContent())
                .contentType(lastMsg.getContentType())
                .senderId(lastMsg.getSenderId())
                .mine(String.valueOf(userId).equals(String.valueOf(lastMsg.getSenderId())))
                .createdAt(lastMsg.getCreatedAt())
                .build();
    }
}
