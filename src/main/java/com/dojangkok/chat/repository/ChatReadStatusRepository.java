package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.ChatReadStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatReadStatusRepository extends MongoRepository<ChatReadStatus, String> {

    Optional<ChatReadStatus> findByRoomIdAndUserId(String roomId, String userId);
}
