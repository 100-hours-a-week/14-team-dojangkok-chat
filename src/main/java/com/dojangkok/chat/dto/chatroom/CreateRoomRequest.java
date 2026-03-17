package com.dojangkok.chat.dto.chatroom;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateRoomRequest {

    private String targetUserId;
    private String propertyId;
}
