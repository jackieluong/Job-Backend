package com.example.Job.service.Impl;

import com.example.Job.config.RedisConfig;
import com.example.Job.service.IRedisService;
import com.example.Job.utils.ClassUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.eclipse.angus.mail.imap.protocol.BODY;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RedisServiceImpl implements IRedisService {
    private static final Logger log = LoggerFactory.getLogger(RedisServiceImpl.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> void set(String key, T value, Long timeout, TimeUnit unit) {
        try{
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Failed to set value in Redis for key: {}, error: {}", key, e.getMessage());
            // Don't rethrow exception - just log and continue
        }


    }

    @Override
    public <T> void set(String key, T value) {

        try{
            redisTemplate.opsForValue().set(key, value, RedisConfig.defaultTTL);
        } catch (Exception e) {
            log.warn("Failed to set value in Redis for key: {}, error: {}", key, e.getMessage());
            // Don't rethrow exception - just log and continue
        }

    }

    @Override
    public <T> void set(String key, T value, Duration duration) {


        try{
            redisTemplate.opsForValue().set(key, value, duration);
        } catch (Exception e) {
            log.warn("Failed to set value in Redis for key: {}, error: {}", key, e.getMessage());
            // Don't rethrow exception - just log and continue
        }
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if(value == null) return null;

            return clazz.cast(value);

        } catch (Exception e) {
            log.warn("Failed to get value from Redis for key: {}, error: {}", key, e.getMessage());
            return null;
        }


    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public Set<String> listKeys(String pattern) {
        return Set.of();
    }

    @Override
    public void addToList(String key, String value) {

    }

    @Override
    public List<String> getList(String key) {
        return List.of();
    }

    @Override
    public <T> boolean addToSet(String key, T value, boolean isRollBack) {
        try{
            redisTemplate.opsForSet().add(key, value);
            return true;
        } catch (Exception e) {
            if(isRollBack){
                throw new RuntimeException(e);
            }

            log.warn("Failed to add value to Redis for key: {}, error: {}", key, e.getMessage());
            return false;
        }
    }


    @Override
    public <T> Set<T> getSet(String key, Class<T> clazz) {
        try{
            Set<Object> members = redisTemplate.opsForSet().members(key);
            if(members == null ) return null;
            else if (members.isEmpty()) return Set.of();

            if(ClassUtil.isSimpleType(clazz)){
                return members.stream().map(clazz::cast).collect(Collectors.toSet());
            }else{
                return members.stream().map(member -> objectMapper.convertValue(member, clazz)).
                        collect(Collectors.toSet());
            }


        } catch (Exception e) {
            log.warn("Failed to get value from Redis for key: {}, error: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> Boolean isMemberOfSet(String key, T value) {
        try{
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (Exception e) {

            log.warn("Failed to check value from Redis for key: {}, error: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> Boolean removeFromSet(String key, T value, boolean isRollBack) {
        try{
            redisTemplate.opsForSet().remove(key, value);
            return true;
        } catch (Exception e) {
            if(isRollBack){
                throw new RuntimeException(e);
            }

            log.warn("Failed to remove value from Redis for key: {}, error: {}", key, e.getMessage());
            return false;
        }
    }


    @Override
    public void setExpiration(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);

    }

    @Override
    public void setExpiration(String key, Duration duration) {
        redisTemplate.expire(key, duration);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
