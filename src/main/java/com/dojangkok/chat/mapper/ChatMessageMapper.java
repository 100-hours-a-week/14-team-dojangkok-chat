package com.dojangkok.chat.mapper;

import com.dojangkok.chat.domain.ChatMessage;
import com.dojangkok.chat.dto.MessageContent;
import com.dojangkok.chat.dto.chatroom.ChatMessageListResponse;
import com.dojangkok.chat.dto.chatroom.ChatMessageResponse;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class ChatMessageMapper {

    public ChatMessageResponse toResponse(ChatMessage message, String userId) {
        return ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .mine(String.valueOf(userId).equals(String.valueOf(message.getSenderId())))
                .contentType(message.getContentType())
                .content(toContentDto(message.getContent()))
                .groupId(message.getGroupId())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public ChatMessageListResponse toListResponse(List<ChatMessageResponse> messages, int requestedSize) {
        boolean hasNext = messages.size() >= requestedSize;

        return ChatMessageListResponse.builder()
                .messages(messages)
                .hasNext(hasNext)
                .nextCursor(hasNext && !messages.isEmpty()
                        ? messages.getLast().getCreatedAt()
                        : null)
                .build();
    }

    public Object toContentDto(MessageContent content) {
        return switch (content) {
            case MessageContent.TextContent text ->
                    ChatMessageResponse.TextDto.builder()
                            .text(text.text())
                            .build();
            case MessageContent.ImageContent img ->
                    ChatMessageResponse.ImageDto.builder()
                            .url(img.url())
                            .width(img.width())
                            .height(img.height())
                            .size(img.size())
                            .build();
            case MessageContent.VideoContent vid ->
                    ChatMessageResponse.VideoDto.builder()
                            .url(vid.url())
                            .duration(vid.duration())
                            .width(vid.width())
                            .height(vid.height())
                            .size(vid.size())
                            .build();
        };
    }
}
