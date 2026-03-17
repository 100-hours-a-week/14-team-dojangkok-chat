package com.dojangkok.chat.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "chat_read_status")
@CompoundIndex(name = "idx_roomId_userId", def = "{'roomId': 1, 'userId': 1}", unique = true)
public class ChatReadStatus {

    @Id
    private String id;
    private String roomId;
    private String userId;
    private String lastReadMessageId;
    private Instant lastReadAt;

    @Builder
    public ChatReadStatus(String roomId, String userId,
                          String lastReadMessageId, Instant lastReadAt) {
        this.roomId = roomId;
        this.userId = userId;
        this.lastReadMessageId = lastReadMessageId;
        this.lastReadAt = lastReadAt;
    }

    public void updateLastRead(String lastReadMessageId, Instant lastReadAt) {
        this.lastReadMessageId = lastReadMessageId;
        this.lastReadAt = lastReadAt;
    }
}
