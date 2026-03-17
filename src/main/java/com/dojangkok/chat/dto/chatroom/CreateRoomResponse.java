package com.dojangkok.chat.dto.chatroom;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CreateRoomResponse {

    private final String roomId;
    private final String type;

    private final PartnerInfoDto partnerInfo;
    private final PropertyInfoDto property;
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
    public static class PropertyInfoDto {
        private final String propertyId;
        private final String title;
        private final String imageUrl;
    }
}
