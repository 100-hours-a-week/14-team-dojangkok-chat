package com.dojangkok.chat.service;

import com.dojangkok.chat.mq.config.RabbitMQChatConfig;
import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.common.exception.GeneralException;
import com.dojangkok.chat.domain.ChatMessage;
import com.dojangkok.chat.domain.ChatReadStatus;
import com.dojangkok.chat.domain.ChatRoom;
import com.dojangkok.chat.dto.event.ChatReadEvent;
import com.dojangkok.chat.repository.ChatMessageRepository;
import com.dojangkok.chat.repository.ChatReadStatusRepository;
import com.dojangkok.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectChatReadStatusService {

    private final ChatReadStatusRepository chatReadStatusRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RabbitTemplate rabbitTemplate;

    public void markAsRead(String userId, String roomId, String lastReadMessageId) {
        Instant readAt = chatMessageRepository.findByMessageId(lastReadMessageId)
                .map(ChatMessage::getCreatedAt)
                .orElse(Instant.now());

        chatReadStatusRepository.findByRoomIdAndUserId(roomId, userId)
                .map(existing -> {
                    if (existing.getLastReadAt() != null && existing.getLastReadAt().isAfter(readAt)) {
                        return existing;
                    }
                    existing.updateLastRead(lastReadMessageId, readAt);
                    return chatReadStatusRepository.save(existing);
                })
                .orElseGet(() -> chatReadStatusRepository.save(
                        ChatReadStatus.builder()
                                .roomId(roomId)
                                .userId(userId)
                                .lastReadMessageId(lastReadMessageId)
                                .lastReadAt(readAt)
                                .build()
                ));
    }

    public void markAsReadAndNotify(String userId, String roomId, String lastReadMessageId) {
        markAsRead(userId, roomId, lastReadMessageId);

        ChatRoom room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new GeneralException(Code.CHAT_ROOM_NOT_FOUND));

        String targetUserId = room.getParticipants().stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElseThrow();

        Instant lastReadAt = chatReadStatusRepository.findByRoomIdAndUserId(roomId, userId)
                .map(ChatReadStatus::getLastReadAt)
                .orElse(Instant.now());

        ChatReadEvent event = new ChatReadEvent(
                roomId,
                userId,
                lastReadMessageId,
                lastReadAt,
                targetUserId
        );

        rabbitTemplate.convertAndSend(RabbitMQChatConfig.CHAT_FANOUT_EXCHANGE, "", event);
        log.info("읽음 이벤트 발행: roomId={}, userId={}", roomId, userId);
    }

    public long getUnreadCount(String userId, String roomId) {
        Instant after = chatReadStatusRepository.findByRoomIdAndUserId(roomId, userId)
                .map(ChatReadStatus::getLastReadAt)
                .orElse(Instant.EPOCH);

        return chatMessageRepository.countUnreadMessages(roomId, userId, after);
    }
}
