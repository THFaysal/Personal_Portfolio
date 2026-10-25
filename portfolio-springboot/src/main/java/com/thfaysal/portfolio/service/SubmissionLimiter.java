package com.thfaysal.portfolio.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SubmissionLimiter {
    private final Cache<String, Boolean> recent = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();

    // Best-effort per-IP cooldown on this application instance. Does not persist visitor IPs.
    public boolean allow(String action, String address) {
        String key = action + ":" + (address == null ? "unknown" : address);
        return recent.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }
}
