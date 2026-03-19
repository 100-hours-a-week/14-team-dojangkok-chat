package com.dojangkok.chat.mapper;

import com.dojangkok.chat.domain.ChatMessage;
import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.MessageContent;
import com.dojangkok.chat.dto.ai.AiChatMessageListResponse;
import com.dojangkok.chat.dto.ai.AiChatMessageResponse;
import com.dojangkok.chat.dto.ai.AiRoomResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiChatRoomMapper {

    public AiRoomResponse toAiRoomResponse(ChatRoom room) {
        return AiRoomResponse.builder()
                .roomId(room.getRoomId())
                .createdAt(room.getCreatedAt())
                .build();
    }

    public AiChatMessageResponse toAiMessageResponse(ChatMessage message) {
        return AiChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .contentType(message.getContentType())
                .content(toContentDto(message.getContent()))
                .createdAt(message.getCreatedAt())
                .build();
    }

    public AiChatMessageListResponse toAiMessageListResponse(List<AiChatMessageResponse> messages, int requestedSize) {
        boolean hasNext = messages.size() >= requestedSize;

        return AiChatMessageListResponse.builder()
                .messages(messages)
                .hasNext(hasNext)
                .nextCursor(hasNext && !messages.isEmpty()
                        ? messages.getLast().getCreatedAt()
                        : null)
                .build();
    }

    private Object toContentDto(MessageContent content) {
        return switch (content) {
            case MessageContent.TextContent text ->
                    AiChatMessageResponse.TextDto.builder()
                            .text(text.text())
                            .build();
            case MessageContent.ImageContent img ->
                    AiChatMessageResponse.ImageDto.builder()
                            .url(img.url())
                            .width(img.width())
                            .height(img.height())
                            .size(img.size())
                            .build();
            case MessageContent.VideoContent vid ->
                    AiChatMessageResponse.VideoDto.builder()
                            .url(vid.url())
                            .duration(vid.duration())
                            .width(vid.width())
                            .height(vid.height())
                            .size(vid.size())
                            .build();
        };
    }
}
