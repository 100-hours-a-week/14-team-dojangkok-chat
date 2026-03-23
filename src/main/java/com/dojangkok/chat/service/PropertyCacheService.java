package com.dojangkok.chat.service;

import com.dojangkok.chat.dto.cache.CachedPropertyInfo;
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
public class PropertyCacheService {

    private static final String KEY_PREFIX = "chat:property:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.main-server.url}")
    private String mainServerUrl;

    @Value("${app.main-server.api-key}")
    private String internalApiKey;

    public CachedPropertyInfo getProperty(String propertyId) {
        // propertyId가 없으면 빈 정보 반환
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
                log.debug("매물 정보 cache hit: propertyId={}", propertyId);
                return objectMapper.readValue(cached, CachedPropertyInfo.class);
            } catch (JsonProcessingException e) {
                log.warn("매물 정보 캐시 역직렬화 실패: propertyId={}", propertyId, e);
                redisTemplate.delete(key);
            }
        }

        // 2. 메인 서버 API 호출
        log.info("매물 정보 cache miss → 메인 서버 호출: propertyId={}", propertyId);
        CachedPropertyInfo property = fetchFromMainServer(propertyId);

        // 3. Redis 캐싱 (실패한 기본값은 캐싱하지 않음)
        if (property != null && !"알 수 없음".equals(property.getTitle())) {
            cacheProperty(key, property);
        }

        return property;
    }

    private CachedPropertyInfo fetchFromMainServer(String propertyId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(mainServerUrl + "/api/internal/properties/{propertyId}", propertyId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(CachedPropertyInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("메인 서버 매물 정보 조회 실패: propertyId={}", propertyId, e);
            return CachedPropertyInfo.builder()
                    .propertyId(propertyId)
                    .title("알 수 없음")
                    .imageUrl(null)
                    .priceMain(null)
                    .priceMonthly(null)
                    .rentType(null)
                    .dealStatus("UNKNOWN")
                    .build();
        }
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
}
