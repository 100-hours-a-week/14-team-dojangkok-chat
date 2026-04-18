package com.dojangkok.chat.dto.chatroom;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatMessageSyncResponse {

    private final List<ChatMessageResponse> messages;
    private final int syncedCount;
}
