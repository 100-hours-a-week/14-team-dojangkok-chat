package com.dojangkok.chat.repository;

import java.time.Instant;

public interface ChatMessageRepositoryCustom {

    long countUnreadMessages(String roomId, String userId, Instant after);
}
