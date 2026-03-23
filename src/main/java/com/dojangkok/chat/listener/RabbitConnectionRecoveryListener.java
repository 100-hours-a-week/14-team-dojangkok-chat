package com.dojangkok.chat.listener;

import com.dojangkok.chat.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitConnectionRecoveryListener implements ConnectionListener {

    private final ConnectionFactory connectionFactory;
    private final WebSocketSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    private final AtomicBoolean initialConnectionDone = new AtomicBoolean(false);

    @PostConstruct
    public void register() {
        connectionFactory.addConnectionListener(this);
        log.info("[MQ Recovery] ConnectionListener 등록 완료");
    }

    @Override
    public void onCreate(Connection connection) {
        if (!initialConnectionDone.getAndSet(true)) {
            log.info("[MQ Recovery] 최초 연결 — SYNC 생략");
            return;
        }

        log.warn("[MQ Recovery] RabbitMQ 재연결 감지 — SYNC_REQUIRED 브로드캐스트");
        broadcastSyncRequired();
    }

    @Override
    public void onClose(Connection connection) {
        log.warn("[MQ Recovery] RabbitMQ 연결 끊김 감지");
    }

    private void broadcastSyncRequired() {
        Set<String> onlineUserIds = sessionManager.getOnlineUserIds();
        if (onlineUserIds.isEmpty()) {
            log.info("[MQ Recovery] 온라인 유저 없음 — 브로드캐스트 생략");
            return;
        }

        Map<String, String> syncMessage = Map.of("type", "SYNC_REQUIRED");

        for (String userId : onlineUserIds) {
            try {
                messagingTemplate.convertAndSendToUser(userId, "/queue/system", syncMessage);
                log.info("[MQ Recovery] SYNC_REQUIRED 전송: userId={}", userId);
            } catch (Exception e) {
                log.error("[MQ Recovery] SYNC_REQUIRED 전송 실패: userId={}", userId, e);
            }
        }

        log.warn("[MQ Recovery] SYNC_REQUIRED 브로드캐스트 완료: {} 명", onlineUserIds.size());
    }
}
