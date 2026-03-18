package com.dojangkok.chat.mapper;

import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.ai.AiRoomResponse;
import org.springframework.stereotype.Component;

@Component
public class AiChatRoomMapper {

    public AiRoomResponse toAiRoomResponse(ChatRoom room, String userId) {
        return AiRoomResponse.builder()
                .roomId(room.getRoomId())
                .type(room.getType())
                .easyContractId(room.getEasyContractId())
                .lastMessage(toLastMessage(room, userId))
                .createdAt(room.getCreatedAt())
                .build();
    }

    private AiRoomResponse.LastMessageDto toLastMessage(ChatRoom room, String userId) {
        if (room.getLastMessage() == null) return null;
        ChatRoom.LastMessage lastMsg = room.getLastMessage();
        return AiRoomResponse.LastMessageDto.builder()
                .content(lastMsg.getContent())
                .contentType(lastMsg.getContentType())
                .senderId(lastMsg.getSenderId())
                .mine(userId.equals(lastMsg.getSenderId()))
                .createdAt(lastMsg.getCreatedAt())
                .build();
    }
}
