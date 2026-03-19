package com.dojangkok.chat.dto.ai;

import com.dojangkok.chat.dto.chatroom.ChatMessageResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AiChatMessageResponse {

    private final String messageId;
    private final String roomId;
    private final String senderId;
    private final String contentType;
    private final Object content;
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
