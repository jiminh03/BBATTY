package com.ssafy.bbatty.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 값 저장 (만료시간 포함)
     */
    public void setValueWithExpiration(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 값 저장 (만료시간 없음)
     */
    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 값 조회
     */
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 값 조회 (타입 지정)
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, Class<T> type) {
        Object value = getValue(key);
        return value != null ? (T) value : null;
    }

    /**
     * 키 존재 여부 확인
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 키 삭제
     */
    public boolean deleteKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 만료시간 설정
     */
    public boolean setExpiration(String key, Duration duration) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, duration));
    }
}