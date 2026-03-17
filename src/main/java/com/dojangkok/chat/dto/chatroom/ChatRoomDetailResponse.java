package com.dojangkok.chat.dto.chatroom;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatRoomDetailResponse {

    private final String roomId;
    private final String type;

    private final PartnerInfoDto partnerInfo;
    private final PropertyDetailDto property;

    private final LastMessageDto lastMessage;
    private final Instant partnerLastReadAt;
    private final Instant createdAt;

    @Getter
    @Builder
    public static class PartnerInfoDto {
        private final String userId;
        private final String nickname;
        private final String profileImageUrl;
    }

    @Getter
    @Builder
    public static class PropertyDetailDto {
        private final String propertyId;
        private final String title;
        private final String imageUrl;
        private final Long priceMain;
        private final Integer priceMonthly;
        private final String rentType;
        private final String dealStatus;
    }

    @Getter
    @Builder
    public static class LastMessageDto {
        private final String content;
        private final String contentType;
        private final String senderId;
        private final boolean mine;
        private final Instant createdAt;
    }
}
