package com.dojangkok.chat.dto.event;

import java.time.Instant;

public record ChatMessageEvent(
        String messageId,
        String roomId,
        String senderId,
        String contentType,
        Object content,
        String groupId,
        Instant createdAt,
        String targetUserId
) implements ChatEvent {
}
