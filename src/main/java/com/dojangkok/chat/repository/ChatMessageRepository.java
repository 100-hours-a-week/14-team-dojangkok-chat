package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String>, ChatMessageRepositoryCustom {

    Optional<ChatMessage> findByMessageId(String messageId);

    List<ChatMessage> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            String roomId, Instant before, Pageable pageable);

    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    long countByRoomIdAndSenderIdNotAndCreatedAtAfter(
            String roomId, String senderId, Instant after);
}
