package com.dojangkok.chat.dto.ai;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AiRoomResponse {

    private final String roomId;
    private final Instant createdAt;
}
