package com.dojangkok.chat.dto.ai;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AiRoomResponse {

    private final String roomId;
    private final String type;
    private final String easyContractId;

    private final LastMessageDto lastMessage;
    private final Instant createdAt;

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
