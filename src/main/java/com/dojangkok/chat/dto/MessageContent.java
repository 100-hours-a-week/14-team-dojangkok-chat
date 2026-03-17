package com.dojangkok.chat.dto;

public sealed interface MessageContent {

    record TextContent(String text) implements MessageContent {
    }

    record ImageContent(String url,
                        int width, int height, long size) implements MessageContent {
    }

    record VideoContent(String url,
                        int duration, int width, int height, long size) implements MessageContent {
    }
}
