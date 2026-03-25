package com.dojangkok.chat.controller;

import com.dojangkok.chat.service.PropertyCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile({"default", "local", "test", "dev"})
@RestController
@RequestMapping("/api/chat/internal/cache")
@RequiredArgsConstructor
public class CacheTestController {

    private final PropertyCacheService propertyCacheService;

    @DeleteMapping("/property/{propertyId}")
    public ResponseEntity<Void> evictProperty(@PathVariable String propertyId) {
        propertyCacheService.evict(propertyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<Map<String, Object>> getProperty(@PathVariable String propertyId) {
        var property = propertyCacheService.getProperty(propertyId);
        return ResponseEntity.ok(Map.of(
                "propertyId", propertyId,
                "title", property.getTitle() != null ? property.getTitle() : "null"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        int hit = propertyCacheService.getCacheHitCount();
        int miss = propertyCacheService.getCacheMissCount();
        int total = hit + miss;
        double hitRate = total > 0 ? (double) hit / total * 100 : 0;

        return ResponseEntity.ok(Map.of(
                "cacheHitCount", hit,
                "cacheMissCount", miss,
                "totalRequests", total,
                "hitRate", String.format("%.2f%%", hitRate)
        ));
    }

    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, Object>> resetStats() {
        int previousHit = propertyCacheService.resetCacheHitCount();
        int previousMiss = propertyCacheService.resetCacheMissCount();
        return ResponseEntity.ok(Map.of(
                "previousCacheHitCount", previousHit,
                "previousCacheMissCount", previousMiss
        ));
    }
}
