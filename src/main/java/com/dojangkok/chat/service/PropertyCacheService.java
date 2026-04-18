package com.dojangkok.chat.service;

import com.dojangkok.chat.common.client.MainServerApiClient;
import com.dojangkok.chat.dto.cache.CachedPropertyInfo;
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
public class PropertyCacheService {

    private static final String KEY_PREFIX = "chat:property:";
    private static final String LOCK_PREFIX = "chat:property:lock:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final int LOCK_RETRY_MAX = 15;
    private static final Duration LOCK_RETRY_WAIT = Duration.ofMillis(50);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MainServerApiClient mainServerApiClient;
    private final Counter hitCounter;
    private final Counter missCounter;

    public PropertyCacheService(StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper,
                                MainServerApiClient mainServerApiClient,
                                MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.mainServerApiClient = mainServerApiClient;
        this.hitCounter = Counter.builder("cache.property.requests")
                .tag("result", "hit")
                .description("Property cache hit count")
                .register(meterRegistry);
        this.missCounter = Counter.builder("cache.property.requests")
                .tag("result", "miss")
                .description("Property cache miss count")
                .register(meterRegistry);
    }
    public CachedPropertyInfo getProperty(String propertyId) {
        if (propertyId == null || propertyId.isBlank()) {
            return CachedPropertyInfo.builder()
                    .propertyId(null)
                    .title(null)
                    .imageUrl(null)
                    .priceMain(null)
                    .priceMonthly(null)
                    .rentType(null)
                    .dealStatus(null)
                    .build();
        }

        String key = KEY_PREFIX + propertyId;

        // 1. Redis 조회
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                incrementHit();
                log.debug("매물 정보 cache hit: propertyId={}", propertyId);
                return objectMapper.readValue(cached, CachedPropertyInfo.class);
            } catch (JsonProcessingException e) {
                log.warn("매물 정보 캐시 역직렬화 실패: propertyId={}", propertyId, e);
                redisTemplate.delete(key);
            }
        }

        // 2. 분산 락으로 스탬피드 방어
        return getPropertyWithLock(propertyId, key);
    }

    // 분산 락 적용: SETNX 뮤텍스로 스탬피드 방어
    private CachedPropertyInfo getPropertyWithLock(String propertyId, String key) {
        String lockKey = LOCK_PREFIX + propertyId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);

        if (Boolean.TRUE.equals(acquired)) {
            try {
                incrementMiss();
                log.info("매물 정보 cache miss → 메인 서버 호출 (락 획득): propertyId={}", propertyId);
                CachedPropertyInfo property = mainServerApiClient.fetchPropertyInfo(propertyId);

                if (property != null && !"알 수 없음".equals(property.getTitle())) {
                    cacheProperty(key, property);
                }

                return property;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        // 락 획득 실패 → 다른 요청이 캐싱 중. 짧게 대기 후 캐시 재조회
        log.debug("매물 캐시 락 대기: propertyId={}", propertyId);
        for (int i = 0; i < LOCK_RETRY_MAX; i++) {
            try {
                Thread.sleep(LOCK_RETRY_WAIT.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                try {
                    incrementHit();
                    return objectMapper.readValue(cached, CachedPropertyInfo.class);
                } catch (JsonProcessingException e) {
                    break;
                }
            }
        }

        // 대기 후에도 캐시 없으면 null 반환 → 호출부에서 기존 스냅샷 사용
        log.warn("매물 캐시 락 대기 실패, 스냅샷 fallback: propertyId={}", propertyId);
        return null;
    }

    private void cacheProperty(String key, CachedPropertyInfo property) {
        try {
            String json = objectMapper.writeValueAsString(property);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("매물 정보 캐시 저장 실패: key={}", key, e);
        }
    }

    public void evict(String propertyId) {
        if (propertyId == null || propertyId.isBlank()) return;
        String key = KEY_PREFIX + propertyId;
        redisTemplate.delete(key);
        log.info("매물 정보 캐시 삭제: propertyId={}", propertyId);
    }

    public double getCacheHitCount() {
        return hitCounter.count();
    }

    public double getCacheMissCount() {
        return missCounter.count();
    }

    // ── Micrometer 카운터 헬퍼 ──

    private void incrementHit() {
        hitCounter.increment();
    }

    private void incrementMiss() {
        missCounter.increment();
    }
}
