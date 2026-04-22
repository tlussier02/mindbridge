package com.digitaltherapy.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    @DisplayName("cacheManager creates a CacheManager with expected caches")
    void cacheManager_CreatesExpectedCaches() {
        CacheConfig config = new CacheConfig();
        CacheManager manager = config.cacheManager();

        assertThat(manager).isNotNull();
        assertThat(manager.getCache("thoughtAnalysis")).isNotNull();
        assertThat(manager.getCache("reframingPrompts")).isNotNull();
        assertThat(manager.getCache("crisisAnalysis")).isNotNull();
        assertThat(manager.getCache("copingStrategies")).isNotNull();
        assertThat(manager.getCache("sessionLibrary")).isNotNull();
        assertThat(manager.getCache("sessionDetails")).isNotNull();
    }

    @Test
    @DisplayName("cacheManager returns six named caches")
    void cacheManager_ReturnsSixCaches() {
        CacheConfig config = new CacheConfig();
        CacheManager manager = config.cacheManager();

        assertThat(manager.getCacheNames()).containsExactlyInAnyOrder(
                "thoughtAnalysis",
                "reframingPrompts",
                "crisisAnalysis",
                "copingStrategies",
                "sessionLibrary",
                "sessionDetails"
        );
    }

    @Test
    @DisplayName("cacheManager can put and get values from caches")
    void cacheManager_PutAndGetValues() {
        CacheConfig config = new CacheConfig();
        CacheManager manager = config.cacheManager();

        var cache = manager.getCache("thoughtAnalysis");
        assertThat(cache).isNotNull();

        cache.put("test-key", "test-value");
        assertThat(cache.get("test-key", String.class)).isEqualTo("test-value");
    }
}
