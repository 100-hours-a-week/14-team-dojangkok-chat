package com.dojangkok.chat.auth;

import com.dojangkok.chat.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("JWT 토큰이 없습니다.");
            }

            // "Bearer " prefix 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String userId = jwtTokenProvider.tryExtractUserIdFromAccessToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 JWT 토큰입니다."));

            // Principal 설정 — 이후 convertAndSendToUser에서 사용됨
            accessor.setUser(new StompPrincipal(userId));
            log.info("WebSocket 연결 인증 성공: userId={}", userId);
        }

        return message;
    }

    public record StompPrincipal(String userId) implements Principal {
        @Override
        public String getName() {
            return userId;
        }
    }
}