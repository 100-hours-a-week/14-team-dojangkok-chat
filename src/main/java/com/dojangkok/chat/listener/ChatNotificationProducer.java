package com.dojangkok.chat.listener;

import com.dojangkok.chat.common.config.RabbitMQChatConfig;
import com.dojangkok.chat.dto.event.ChatNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendNotification(ChatNotificationEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQChatConfig.NOTIFICATION_EXCHANGE,
                "chat.notification",
                event
        );
        log.info("채팅 알림 발행: targetMemberId={}, roomId={}, messageId={}",
                event.targetMemberId(), event.roomId(), event.messageId());
    }
}
