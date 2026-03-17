package com.dojangkok.chat.domain;

import com.dojangkok.chat.dto.MessageContent;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "chat_messages")
@CompoundIndex(name = "idx_roomId_createdAt", def = "{'roomId': 1, 'createdAt': -1}")
public class ChatMessage {

    @Id
    private String id;
    private String messageId;
    private String roomId;
    private String senderId;
    private String contentType;
    private MessageContent content;
    private String groupId;
    private Instant createdAt;

    @Builder
    public ChatMessage(String messageId, String roomId, String senderId,
                       String contentType, MessageContent content, String groupId,
                       Instant createdAt) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderId = senderId;
        this.contentType = contentType;
        this.content = content;
        this.groupId = groupId;
        this.createdAt = createdAt;
    }
}
