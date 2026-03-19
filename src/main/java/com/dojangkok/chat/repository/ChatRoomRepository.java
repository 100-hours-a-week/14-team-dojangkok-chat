package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    Optional<ChatRoom> findByRoomId(String roomId);

    // 순서 무관하게 participants 매칭함 -> $all(모두 포함) + $size(정확히 2명)
    @Query("{ 'type': ?0, 'participants': { $all: ?1, $size: 2 }, 'propertyId': ?2 }")
    Optional<ChatRoom> findExistingRoom(String type, List<String> participants, String propertyId);

    List<ChatRoom> findByParticipantsContainingAndLastMessageIsNotNullOrderByLastMessage_CreatedAtDesc(String userId);

    Optional<ChatRoom> findByTypeAndParticipantsContainingAndEasyContractId(String type, String userId, String easyContractId);
}
