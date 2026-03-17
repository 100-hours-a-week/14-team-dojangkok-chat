package com.dojangkok.chat.service;

import com.dojangkok.chat.dto.cache.CachedUserProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileCacheService {

    private static final String KEY_PREFIX = "chat:user:profile:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.main-server.url}")
    private String mainServerUrl;

    @Value("${app.main-server.api-key}")
    private String internalApiKey;

    public CachedUserProfile getProfile(String userId) {
        String key = KEY_PREFIX + userId;

        // 1. Redis 조회
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                log.debug("유저 프로필 cache hit: userId={}", userId);
                return objectMapper.readValue(cached, CachedUserProfile.class);
            } catch (JsonProcessingException e) {
                log.warn("유저 프로필 캐시 역직렬화 실패: userId={}", userId, e);
                redisTemplate.delete(key);
            }
        }

        // 2. 메인 서버 API 호출
        log.info("유저 프로필 cache miss → 메인 서버 호출: userId={}", userId);
        CachedUserProfile profile = fetchFromMainServer(userId);

        // 3. Redis 캐싱 (실패한 기본값은 캐싱하지 않음)
        if (profile != null && !"알 수 없음".equals(profile.getNickname())) {
            cacheProfile(key, profile);
        }

        return profile;
    }

    private CachedUserProfile fetchFromMainServer(String userId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(mainServerUrl + "/api/internal/users/{userId}", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(CachedUserProfile.class)
                    .block();
        } catch (Exception e) {
            log.error("메인 서버 유저 프로필 조회 실패: userId={}", userId, e);
            // 메인 서버 호출 실패 시 기본값 반환
            return CachedUserProfile.builder()
                    .userId(userId)
                    .nickname("알 수 없음")
                    .profileImageUrl(null)
                    .build();
        }
    }

    private void cacheProfile(String key, CachedUserProfile profile) {
        try {
            String json = objectMapper.writeValueAsString(profile);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("유저 프로필 캐시 저장 실패: key={}", key, e);
        }
    }
}
