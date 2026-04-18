package com.dojangkok.chat.controller;

import com.dojangkok.chat.service.PropertyCacheService;
import com.dojangkok.chat.service.PropertyCacheLegacyService;
import com.dojangkok.chat.service.UserProfileCacheService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Profile({"default", "local", "test", "dev"})
@RestController
@RequestMapping("/api/chat/internal/cache")
@RequiredArgsConstructor
public class CacheTestController {

    private final PropertyCacheService propertyCacheService;
    private final PropertyCacheLegacyService propertyCacheLegacyService;
    private final MeterRegistry meterRegistry;

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
        Map<String, Object> stats = new LinkedHashMap<>();

        // Property cache (with lock)
        addCacheStats(stats, "property", "cache.property.requests");

        // Property cache (legacy, no lock)
        addCacheStats(stats, "propertyLegacy", "cache.property.legacy.requests");

        // User profile cache
        addCacheStats(stats, "userProfile", "cache.user.profile.requests");

        return ResponseEntity.ok(stats);
    }

    private void addCacheStats(Map<String, Object> stats, String prefix, String metricName) {
        double hit = getCounterValue(metricName, "hit");
        double miss = getCounterValue(metricName, "miss");
        double total = hit + miss;
        double hitRate = total > 0 ? hit / total * 100 : 0;

        stats.put(prefix + "Hit", (long) hit);
        stats.put(prefix + "Miss", (long) miss);
        stats.put(prefix + "Total", (long) total);
        stats.put(prefix + "HitRate", String.format("%.2f%%", hitRate));
    }

    private double getCounterValue(String metricName, String resultTag) {
        Counter counter = meterRegistry.find(metricName)
                .tag("result", resultTag)
                .counter();
        return counter != null ? counter.count() : 0;
    }
}
