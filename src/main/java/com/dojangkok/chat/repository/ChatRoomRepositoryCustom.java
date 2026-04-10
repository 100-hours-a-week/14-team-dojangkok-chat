package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.ChatRoom;

import java.time.Instant;
import java.util.List;


public interface ChatRoomRepositoryCustom {

    // 참여자 프로필 스냅샷만 $set으로 부분 업데이트
    void updateParticipantProfiles(String roomId,
                                   List<ChatRoom.ParticipantInfo> profiles,
                                   Instant eventTimestamp);

    // 매물 스냅샷(제목, 이미지)만 $set으로 부분 업데이트
    void updatePropertySnapshot(String roomId,
                                String propertyTitle,
                                String propertyImageUrl,
                                Instant eventTimestamp);

    // lastMessage만 $set으로 부분 업데이트
    void updateLastMessage(String roomId, ChatRoom.LastMessage lastMessage);
}
