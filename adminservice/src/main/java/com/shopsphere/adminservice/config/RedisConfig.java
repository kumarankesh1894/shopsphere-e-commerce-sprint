package com.shopsphere.adminservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
/*
 * What:
 * Redis cache configuration for adminservice.
 *
 * Why:
 * Dashboard and reports perform heavy aggregation, so caching improves response time.
 *
 * How:
 * - Use RedisCacheManager with JSON serializer.
 * - Configure TTL for each admin cache.
 * - Handle cache errors gracefully (fail-open).
 */
public class RedisConfig implements CachingConfigurer {

    /*
     * What:
     * Builds Redis cache manager.
     *
     * Why:
     * We need JSON serialization for DTO responses (JDK serialization caused runtime errors).
     *
     * How:
     * 1) Copy ObjectMapper and enable type metadata.
     * 2) Configure GenericJackson2JsonRedisSerializer.
     * 3) Set default + per-cache TTL settings.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        /*
         * Copy Spring's auto-configured ObjectMapper so we keep all registered modules
         * (JavaTimeModule for LocalDateTime, etc.) while adding type metadata for Redis.
         * Using new ObjectMapper() directly would lose those modules and cause
         * deserialization failures for date/time fields stored in report DTOs.
         */
        ObjectMapper redisObjectMapper = objectMapper.copy();
        /*
         * activateDefaultTyping embeds class type metadata inside the JSON payload.
         * This allows Redis to deserialize back to the correct Java type
         * (e.g. DashboardResponse, List<TopProductDto>) without ambiguity.
         */
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        /*
         * Serializer used by Spring Cache for all Redis values in this service:
         * - Java object → JSON when writing to Redis
         * - JSON → Java object when reading from Redis
         */
        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper));

        /*
         * Default cache policy: JSON serializer + 5-minute TTL.
         * Any cache not listed in cacheConfigs below falls back to this.
         */
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(jsonSerializer)
                .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        // dashboard: aggregated revenue + order counts + top products
        cacheConfigs.put("dashboard", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        // sales: day-by-day revenue breakdown for a date range
        cacheConfigs.put("sales", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        // adminReportProducts: top products by revenue for a date range.
        // Renamed from "products" to avoid collision with catalogservice's "products"
        // cache which stores individual ProductResponse DTOs keyed by product ID.
        // Both services share the same Redis instance, so names must be unique.
        cacheConfigs.put("adminReportProducts", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        // users: customer activity summary (spend, order count) for a date range
        cacheConfigs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        // orders: full admin order list used by the order management table
        cacheConfigs.put("orders", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /*
     * What:
     * Custom cache error handler.
     *
     * Why:
     * Cache failures should not break API responses.
     *
     * How:
     * Log cache errors and allow normal service flow to continue.
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.warn("cache.error.get cache={} key={} message={}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
                log.warn("cache.error.put cache={} key={} message={}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.warn("cache.error.evict cache={} key={} message={}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                log.warn("cache.error.clear cache={} message={}", cache.getName(), exception.getMessage());
            }
        };
    }

    /*
     * What:
     * Registers the custom cache error handler with Spring caching.
     *
     * Why:
     * Ensures @Cacheable/@CacheEvict paths use our fail-open handler.
     *
     * How:
     * Return cacheErrorHandler() from CachingConfigurer.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler();
    }
}

