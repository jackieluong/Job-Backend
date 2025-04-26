package com.example.Job.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfig {

    // default cache time to live will be 7 days, if no time to live is specified
    public static final Duration defaultTTL = Duration.ofDays(7);


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Serializer cho key và value
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // explicitly enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();

        return template;
    }

    /**
        * Generates a Redis key for an entity based on its type, field, and value.
        * Format: <entity>:<field>:<value>
        * Example: user:email:john@example.com, job:id:123
     */
    public static String generateKey(Class<?> entityClass, String field, Object value){
        if (entityClass == null || field == null || value == null) {
            throw new IllegalArgumentException("Entity class, field, and value cannot be null");
        }

        String entityName = entityClass.getSimpleName().toLowerCase();
        return String.format("%s:%s:%s", entityName, field, value);
    }



}

