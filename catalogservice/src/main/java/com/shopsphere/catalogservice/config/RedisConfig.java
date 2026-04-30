package com.shopsphere.catalogservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {

        /*
         * Copy Spring's auto-configured ObjectMapper so we keep all registered modules
         * (JavaTimeModule for LocalDateTime, etc.) while adding type metadata for Redis.
         * Using new ObjectMapper() directly would lose those modules and cause
         * deserialization failures for date/time fields.
         */
        ObjectMapper redisObjectMapper = objectMapper.copy();
        // preserve class type in Redis so deserialization knows the target type
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        /*
         * Create a JSON serializer for Redis
         *
         * GenericJackson2JsonRedisSerializer:
         * - Converts Java objects → JSON before storing in Redis
         * - Converts JSON → Java objects when reading from Redis
         *
         * Why we pass ObjectMapper?
         * - To control how JSON serialization/deserialization happens
         * - Supports custom configurations if needed (date format, etc.)
         */
        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper));

        // Default config (fallback for any cache not listed below)
        RedisCacheConfiguration defaultConfig = defaultCacheConfig()
                .serializeValuesWith(jsonSerializer)
                .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Product by ID — TTL aligned with productsList to prevent list/detail inconsistency
        cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Product list (paginated)
        cacheConfigs.put("productsList", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Search results — slightly shorter since filters make results more dynamic
        cacheConfigs.put("productSearch", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
