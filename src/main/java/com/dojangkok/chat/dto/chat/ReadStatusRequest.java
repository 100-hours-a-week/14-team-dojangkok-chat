package com.dojangkok.chat.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReadStatusRequest {

    private String roomId;
    private String lastReadMessageId;
}
