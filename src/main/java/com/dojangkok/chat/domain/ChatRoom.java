package com.dojangkok.chat.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Document(collection = "chat_rooms")
@CompoundIndexes({
        @CompoundIndex(name = "idx_type_participants_property", def = "{'type': 1, 'participants': 1, 'propertyId': 1}", unique = true),
        @CompoundIndex(name = "idx_participants_lastmsg", def = "{'participants': 1, 'lastMessage.createdAt': -1}")
})
public class ChatRoom {

    @Id
    private String id;
    private String roomId;
    private String type;
    private List<String> participants;

    // 매물 스냅샷
    private String propertyId;
    private String propertyTitle;
    private String propertyImageUrl;

    // 참여자 프로필 스냅샷
    private List<ParticipantInfo> participantProfiles;

    private LastMessage lastMessage;
    private Instant createdAt;

    @Getter
    @Builder
    public static class ParticipantInfo {
        private String userId;
        private String nickname;
        private String profileImageUrl;
    }

    @Getter
    @Builder
    public static class LastMessage {
        private String content;
        private String contentType;
        private String senderId;
        private Instant createdAt;
    }

    @Builder
    public ChatRoom(String roomId, String type, List<String> participants,
                    String propertyId, String propertyTitle, String propertyImageUrl,
                    List<ParticipantInfo> participantProfiles,
                    LastMessage lastMessage, Instant createdAt) {
        this.roomId = roomId;
        this.type = type;
        this.participants = participants;
        this.propertyId = propertyId;
        this.propertyTitle = propertyTitle;
        this.propertyImageUrl = propertyImageUrl;
        this.participantProfiles = participantProfiles;
        this.lastMessage = lastMessage;
        this.createdAt = createdAt;
    }

    public void updateLastMessage(LastMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void updateParticipantProfiles(List<ParticipantInfo> participantProfiles) {
        this.participantProfiles = participantProfiles;
    }

    public void updatePropertySnapshot(String propertyTitle, String propertyImageUrl) {
        this.propertyTitle = propertyTitle;
        this.propertyImageUrl = propertyImageUrl;
    }
}
