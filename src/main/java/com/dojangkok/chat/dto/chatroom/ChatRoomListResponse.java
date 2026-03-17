package com.dojangkok.chat.dto.chatroom;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomListResponse {

    private final int totalCount;
    private final List<ChatRoomResponse> rooms;
}
