package com.ssafy.bbatty.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void setValue(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    public <T> T getValue(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    public void increment(String key, long delta) {
        redisTemplate.opsForValue().increment(key, delta);
    }

    public void decrement(String key) {
        redisTemplate.opsForValue().decrement(key);
    }

    public void decrement(String key, long delta) {
        redisTemplate.opsForValue().decrement(key, delta);
    }
    
    /**
     * Set에 값 추가
     */
    public void addToSet(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
    }
    
    /**
     * Set에서 값 제거
     */
    public void removeFromSet(String key, Object value) {
        redisTemplate.opsForSet().remove(key, value);
    }
    
    /**
     * Set에 값이 있는지 확인
     */
    public boolean isMemberOfSet(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }
    
    /**
     * Set의 크기 반환
     */
    public Long getSetSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }
    
    /**
     * 키에 TTL 설정
     */
    public void expire(String key, Duration timeout) {
        redisTemplate.expire(key, timeout);
    }
}