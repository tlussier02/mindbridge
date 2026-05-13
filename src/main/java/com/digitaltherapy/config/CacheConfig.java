package com.digitaltherapy.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);

        // LLM response caches — 60 min TTL
        manager.registerCustomCache("thoughtAnalysis",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(60, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
        manager.registerCustomCache("reframingPrompts",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(60, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Crisis analysis — shorter TTL for safety
        manager.registerCustomCache("crisisAnalysis",
                Caffeine.newBuilder()
                        .maximumSize(50)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Static data caches — 24 hr TTL
        manager.registerCustomCache("copingStrategies",
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .recordStats()
                        .build());
        manager.registerCustomCache("sessionLibrary",
                Caffeine.newBuilder()
                        .maximumSize(10)
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .recordStats()
                        .build());
        manager.registerCustomCache("sessionDetails",
                Caffeine.newBuilder()
                        .maximumSize(20)
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .recordStats()
                        .build());

        return manager;
    }
}
