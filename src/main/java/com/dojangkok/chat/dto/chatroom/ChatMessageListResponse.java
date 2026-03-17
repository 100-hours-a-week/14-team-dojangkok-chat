package com.dojangkok.chat.dto.chatroom;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ChatMessageListResponse {

    private final List<ChatMessageResponse> messages;
    private final boolean hasNext;
    private final Instant nextCursor;
}
