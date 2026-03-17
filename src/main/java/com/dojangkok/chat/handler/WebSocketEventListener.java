package com.dojangkok.chat.handler;

import com.dojangkok.chat.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (user != null && sessionId != null) {
            sessionManager.registerSession(user.getName(), sessionId);
            log.info("[WS] 연결: sessionId={}, user={}", sessionId, user.getName());
        }
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("[WS] 구독: sessionId={}, destination={}, user={}",
                accessor.getSessionId(), accessor.getDestination(),
                accessor.getUser() != null ? accessor.getUser().getName() : "unknown");
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (user != null && sessionId != null) {
            sessionManager.removeSession(user.getName(), sessionId);
            log.info("[WS] 연결 해제: sessionId={}, user={}", sessionId, user.getName());
        }
    }
}
