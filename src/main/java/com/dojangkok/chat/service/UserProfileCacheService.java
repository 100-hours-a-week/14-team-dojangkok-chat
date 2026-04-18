package com.dojangkok.chat.service;

import com.dojangkok.chat.common.client.MainServerApiClient;
import com.dojangkok.chat.dto.cache.CachedUserProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class UserProfileCacheService {

    private static final String KEY_PREFIX = "chat:user:profile:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MainServerApiClient mainServerApiClient;
    private final Counter hitCounter;
    private final Counter missCounter;

    public UserProfileCacheService(StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper,
                                   MainServerApiClient mainServerApiClient,
                                   MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.mainServerApiClient = mainServerApiClient;
        this.hitCounter = Counter.builder("cache.user.profile.requests")
                .tag("result", "hit")
                .description("User profile cache hit count")
                .register(meterRegistry);
        this.missCounter = Counter.builder("cache.user.profile.requests")
                .tag("result", "miss")
                .description("User profile cache miss count")
                .register(meterRegistry);
    }

    public CachedUserProfile getProfile(String userId) {
        String key = KEY_PREFIX + userId;

        // 1. Redis 조회
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                hitCounter.increment();
                log.debug("유저 프로필 cache hit: userId={}", userId);
                return objectMapper.readValue(cached, CachedUserProfile.class);
            } catch (JsonProcessingException e) {
                log.warn("유저 프로필 캐시 역직렬화 실패: userId={}", userId, e);
                redisTemplate.delete(key);
            }
        }

        // 2. 메인 서버 API 호출 (서킷 브레이커 적용)
        missCounter.increment();
        log.info("유저 프로필 cache miss → 메인 서버 호출: userId={}", userId);
        CachedUserProfile profile = mainServerApiClient.fetchUserProfile(userId);

        // 3. Redis 캐싱 (실패한 기본값은 캐싱하지 않음)
        if (profile != null && !"알 수 없음".equals(profile.getNickname())) {
            cacheProfile(key, profile);
        }

        return profile;
    }

    private void cacheProfile(String key, CachedUserProfile profile) {
        try {
            String json = objectMapper.writeValueAsString(profile);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("유저 프로필 캐시 저장 실패: key={}", key, e);
        }
    }

    public void evict(String userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("유저 프로필 캐시 삭제: userId={}", userId);
    }
}
