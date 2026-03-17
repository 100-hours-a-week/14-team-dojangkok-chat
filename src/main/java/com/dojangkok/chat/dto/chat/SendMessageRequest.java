package com.dojangkok.chat.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendMessageRequest {

    private String roomId;
    private String contentType;
    private String groupId;

    // TEXT
    private String text;

    // IMAGE, VIDEO 공통
    private String url;
    private int width;
    private int height;
    private long size;

    // VIDEO 전용
    private int duration;
}
