package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    Optional<ChatRoom> findByRoomId(String roomId);

    Optional<ChatRoom> findByTypeAndParticipants(String type, List<String> participants);

    Optional<ChatRoom> findByTypeAndParticipantsAndPropertyId(String type, List<String> participants, String propertyId);

    List<ChatRoom> findByParticipantsContainingOrderByLastMessage_CreatedAtDesc(String userId);

    List<ChatRoom> findByParticipantsContainingAndLastMessageIsNotNullOrderByLastMessage_CreatedAtDesc(String userId);
}
