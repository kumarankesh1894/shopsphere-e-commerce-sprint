package com.shopsphere.orderservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
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

import static org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig;

@Configuration
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {

        /*
         * We use Redis as a shared cache for read-heavy APIs.
         *
         * Why copy ObjectMapper?
         * - Spring already configures ObjectMapper with useful modules
         *   (for example Java date/time support).
         * - We copy it so cache-specific typing rules do not affect
         *   the global JSON behavior used by controllers.
         */

        ObjectMapper redisObjectMapper = objectMapper.copy();

        /*
         * Redis stores values as JSON text.
         *
         * activateDefaultTyping adds class type metadata inside JSON.
         * This helps Redis convert JSON back to the correct Java type
         * when we read from cache (DTOs, lists, wrapper objects, etc.).
         */
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        /*
         * Serializer used by Spring Cache for Redis values:
         * - Java object -> JSON while writing to Redis
         * - JSON -> Java object while reading from Redis
         */
        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper));

        /*
         * Default cache behavior for this service:
         * - Use JSON serializer above
         * - Keep entries for 5 minutes unless a cache overrides TTL
         */
        RedisCacheConfiguration defaultConfig = defaultCacheConfig()
                .serializeValuesWith(jsonSerializer)
                .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        /*
         * Per-cache TTL settings.
         *
         * orderByUser:
         * - Single order details by user and order id
         * - Medium TTL is good because data is read often
         *
         * orderHistory:
         * - Paginated my-orders list
         * - Slightly shorter TTL because list changes more often
         */
        cacheConfigs.put("orderByUser", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("orderHistory", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        /*
         * Build RedisCacheManager with:
         * - one default policy
         * - cache-specific overrides
         */
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        /*
         * Fail-open cache strategy:
         * If Redis has a temporary issue (network, serialization, timeout),
         * we log a warning and allow the request to continue.
         *
         * Result:
         * - API stays available
         * - Data is fetched from database when cache fails
         */
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.warn("cache.error.get cache={} key={} message={}", safeCacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
                log.warn("cache.error.put cache={} key={} message={}", safeCacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.warn("cache.error.evict cache={} key={} message={}", safeCacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                log.warn("cache.error.clear cache={} message={}", safeCacheName(cache), exception.getMessage());
            }

            private String safeCacheName(Cache cache) {
                return cache != null ? cache.getName() : "unknown";
            }
        };
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler();
    }
}

