package com.dojangkok.chat.listener;

import com.dojangkok.chat.dto.event.ChatEvent;
import com.dojangkok.chat.dto.event.ChatMessageEvent;
import com.dojangkok.chat.dto.event.ChatReadEvent;
import com.dojangkok.chat.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageListener {

    private final WebSocketSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "#{chatInstanceQueue.name}")
    public void handleChatEvent(ChatEvent event) {
        String targetUserId = event.targetUserId();

        if (!sessionManager.isUserOnline(targetUserId)) {
            return;
        }

        if (event instanceof ChatMessageEvent messageEvent) {
            messagingTemplate.convertAndSendToUser(
                    targetUserId, "/queue/messages", messageEvent);
            log.info("메시지 전달: targetUserId={}, roomId={}", targetUserId, messageEvent.roomId());
        } else if (event instanceof ChatReadEvent readEvent) {
            messagingTemplate.convertAndSendToUser(
                    targetUserId, "/queue/notifications", readEvent);
            log.info("읽음 상태 전달: targetUserId={}, roomId={}", targetUserId, readEvent.roomId());
        }
    }
}
