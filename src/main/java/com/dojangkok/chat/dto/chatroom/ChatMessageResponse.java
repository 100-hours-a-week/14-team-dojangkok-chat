package com.dojangkok.chat.dto.chatroom;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatMessageResponse {

    private final String messageId;
    private final String roomId;
    private final String senderId;
    private final boolean mine;
    private final String contentType;
    private final Object content;
    private final String groupId;
    private final Instant createdAt;

    @Getter
    @Builder
    public static class TextDto {
        private final String text;
    }

    @Getter
    @Builder
    public static class ImageDto {
        private final String url;
        private final int width;
        private final int height;
        private final long size;
    }

    @Getter
    @Builder
    public static class VideoDto {
        private final String url;
        private final int duration;
        private final int width;
        private final int height;
        private final long size;
    }
}
