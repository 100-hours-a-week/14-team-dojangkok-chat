package com.dojangkok.chat.mq;

import com.dojangkok.chat.dto.event.ChatNotificationEvent;
import com.dojangkok.chat.mq.config.RabbitMQChatConfig;
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
                "quorum.notification",
                event
        );
        log.info("채팅 알림 발행: targetMemberId={}, roomId={}, messageId={}",
                event.targetMemberId(), event.roomId(), event.messageId());
    }
}
