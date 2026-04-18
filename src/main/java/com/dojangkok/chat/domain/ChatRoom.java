package com.dojangkok.chat.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Document(collection = "chat_rooms")
public class ChatRoom {

    @Id
    private String id;
    private String roomId;
    private String type;
    private List<String> participants;

    // 중복 방지용: "DIRECT:1:2:property:4" 또는 "AI:3:AI_ASSISTANT:contract:212" 형태
    // 배열에는 unique index를 걸 수 없으므로, 이 스칼라 필드로 유니크 보장
    private String roomKey;

    // 매물 스냅샷
    private String propertyId;
    private String propertyTitle;
    private String propertyImageUrl;

    // AI 채팅방용 — 쉬운 계약서 ID
    private String easyContractId;

    // 참여자 프로필 스냅샷
    private List<ParticipantInfo> participantProfiles;

    private LastMessage lastMessage;
    private Instant createdAt;

    // 이벤트 순서 보장용: 마지막으로 처리된 data.events 이벤트 시점
    private Instant lastEventTimestamp;

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
                    String easyContractId,
                    List<ParticipantInfo> participantProfiles,
                    LastMessage lastMessage, Instant createdAt) {
        this.roomId = roomId;
        this.type = type;
        this.participants = participants;
        this.roomKey = buildRoomKey(type, participants, propertyId, easyContractId);
        this.propertyId = propertyId;
        this.propertyTitle = propertyTitle;
        this.propertyImageUrl = propertyImageUrl;
        this.easyContractId = easyContractId;
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

    public void markEventTimestamp(Instant eventTimestamp) {
        this.lastEventTimestamp = eventTimestamp;
    }

    public boolean isStaleEvent(Instant eventTimestamp) {
        if (eventTimestamp == null) return false;
        if (this.lastEventTimestamp == null) return false;
        return eventTimestamp.isBefore(this.lastEventTimestamp);
    }

    /**
     * 채팅방 유니크 키 생성
     * DIRECT 방: "DIRECT:1:2:property:4"
     * AI 방:     "AI:3:AI_ASSISTANT:contract:212"
     */
    private static String buildRoomKey(String type, List<String> participants,
                                        String propertyId, String easyContractId) {
        if (participants == null || type == null) return null;
        String sortedParticipants = participants.stream().sorted().collect(Collectors.joining(":"));
        if (propertyId != null) {
            return type + ":" + sortedParticipants + ":property:" + propertyId;
        }
        if (easyContractId != null) {
            return type + ":" + sortedParticipants + ":contract:" + easyContractId;
        }
        return type + ":" + sortedParticipants;
    }
}
