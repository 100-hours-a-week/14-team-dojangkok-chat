package com.dojangkok.chat.dto.ai;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AiChatMessageListResponse {

    private final List<AiChatMessageResponse> messages;
    private final boolean hasNext;
    private final Instant nextCursor;
}
