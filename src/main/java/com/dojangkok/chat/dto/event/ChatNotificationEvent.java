package com.dojangkok.chat.dto.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ChatNotificationEvent(
        String type,
        String messageId,
        String roomId,
        String senderId,
        String senderNickname,
        String senderProfileImageUrl,
        Long targetMemberId,
        String contentType,
        String preview,
        Instant createdAt
) {
}
