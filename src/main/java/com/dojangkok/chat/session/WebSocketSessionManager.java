package com.dojangkok.chat.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {

    // userId → sessionId 집합 (한 유저가 여러 탭/기기로 접속할 수 있음)
    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    public void registerSession(String userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("세션 등록: userId={}, sessionId={}", userId, sessionId);
    }

    public void removeSession(String userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.info("세션 제거: userId={}, sessionId={}", userId, sessionId);
    }

    public boolean isUserOnline(String userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<String> getOnlineUserIds() {
        return userSessions.keySet();
    }
}
