package com.dojangkok.chat.common.client;

import com.dojangkok.chat.dto.cache.CachedPropertyInfo;
import com.dojangkok.chat.dto.cache.CachedUserProfile;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainServerApiClient {

    private final RestClient mainServerRestClient;

    @CircuitBreaker(name = "mainServerApi", fallbackMethod = "fallbackUserProfile")
    public CachedUserProfile fetchUserProfile(String userId) {
        return mainServerRestClient.get()
                .uri("/api/internal/users/{userId}", userId)
                .retrieve()
                .body(CachedUserProfile.class);
    }

    @CircuitBreaker(name = "mainServerApi", fallbackMethod = "fallbackPropertyInfo")
    public CachedPropertyInfo fetchPropertyInfo(String propertyId) {
        return mainServerRestClient.get()
                .uri("/api/internal/properties/{propertyId}", propertyId)
                .retrieve()
                .body(CachedPropertyInfo.class);
    }

    private CachedUserProfile fallbackUserProfile(String userId, Throwable t) {
        log.warn("서킷 브레이커 fallback — 유저 프로필: userId={}, reason={}", userId, t.getMessage());
        return CachedUserProfile.builder()
                .userId(userId)
                .nickname("알 수 없음")
                .profileImageUrl(null)
                .build();
    }

    private CachedPropertyInfo fallbackPropertyInfo(String propertyId, Throwable t) {
        log.warn("서킷 브레이커 fallback — 매물 정보: propertyId={}, reason={}", propertyId, t.getMessage());
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
