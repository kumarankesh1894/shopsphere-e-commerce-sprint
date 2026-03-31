package com.shopsphere.adminservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    void cacheManager_buildsWithConfiguredCaches() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory, new ObjectMapper());

        assertNotNull(cacheManager);
        assertNotNull(cacheManager.getCache("dashboard"));
        assertNotNull(cacheManager.getCache("sales"));
        assertNotNull(cacheManager.getCache("products"));
        assertNotNull(cacheManager.getCache("users"));
        assertNotNull(cacheManager.getCache("orders"));
    }

    @Test
    void cacheErrorHandler_methodsAreFailOpen() {
        CacheErrorHandler handler = redisConfig.cacheErrorHandler();
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("dashboard");
        RuntimeException ex = new RuntimeException("redis down");

        assertDoesNotThrow(() -> handler.handleCacheGetError(ex, cache, "k1"));
        assertDoesNotThrow(() -> handler.handleCachePutError(ex, cache, "k1", "v1"));
        assertDoesNotThrow(() -> handler.handleCacheEvictError(ex, cache, "k1"));
        assertDoesNotThrow(() -> handler.handleCacheClearError(ex, cache));
    }

    @Test
    void errorHandler_returnsConfiguredHandler() {
        assertNotNull(redisConfig.errorHandler());
    }
}

