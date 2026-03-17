package com.dojangkok.chat.dto.event;

import java.time.Instant;

public record ChatReadEvent(
        String roomId,
        String userId,
        String lastReadMessageId,
        Instant lastReadAt,
        String targetUserId
) implements ChatEvent {
}
