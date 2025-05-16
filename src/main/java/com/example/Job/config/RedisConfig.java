package com.example.Job.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUserName;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    // default cache time to live will be 2 days, if no time to live is specified
    public static final Duration defaultTTL = Duration.ofDays(2);

//    @Bean(destroyMethod = "shutdown")
//    public ClientResources clientResources() {
//        return DefaultClientResources.create();
//    }

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(/*ClientResources clientResources*/) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(redisPassword);
        config.setUsername(redisUserName);
//        config.setDatabase(0); // Redis database index

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxWaitMillis(3000); // Maximum wait time for obtaining a connection
        poolConfig.setMaxIdle(8); // Maximum number of idle connections
        poolConfig.setMinIdle(4); // Minimum number of idle connections
        poolConfig.setMaxTotal(10); // Maximum number of connections

        LettucePoolingClientConfiguration poolingClientConfig =
                LettucePoolingClientConfiguration.builder()
//                        .clientResources(clientResources)
                        .commandTimeout(Duration.ofMillis(3000)) // Command timeout
                        .poolConfig(poolConfig)  // Set connection pool configuration
                        .build();


        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(config, poolingClientConfig);
//        lettuceConnectionFactory.setShareNativeConnection(false);
        return lettuceConnectionFactory;
    }

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

