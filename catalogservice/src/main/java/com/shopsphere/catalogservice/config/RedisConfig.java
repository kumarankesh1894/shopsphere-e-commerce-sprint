package com.shopsphere.catalogservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static com.netflix.spectator.impl.Config.defaultConfig;
import static org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig;

@Configuration
public class RedisConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // JSON Serializer
        // Create ObjectMapper instance (used for JSON conversion i.e java-> json and json->java)
        ObjectMapper objectMapper = new ObjectMapper();
        //preserve class type in Redis
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
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
                        //It serializes Java objects into JSON format for storing in Redis
                        // and deserializes them back, making the cache readable and microservice-friendly.
                        new GenericJackson2JsonRedisSerializer(objectMapper));

        // 🔥 Default config (fallback)
        RedisCacheConfiguration defaultConfig = defaultCacheConfig()
                .serializeValuesWith(jsonSerializer)
                .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Product by ID (stable)
        cacheConfigs.put("products",defaultConfig
                        .entryTtl(Duration.ofMinutes(10)));

        // Product list
        cacheConfigs.put("productsList",
                defaultConfig
                        .entryTtl(Duration.ofMinutes(3)));

        // Search (very dynamic)
        cacheConfigs.put("productSearch",
                defaultConfig
                        .entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }



}
