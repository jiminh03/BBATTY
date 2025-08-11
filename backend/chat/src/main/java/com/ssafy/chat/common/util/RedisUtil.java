package com.ssafy.chat.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
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
    
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
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

    // Hash 관련 메서드들
    public void setHashValue(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public void setHashValue(String key, String hashKey, Object value, Duration timeout) {
        redisTemplate.opsForHash().put(key, hashKey, value);
        redisTemplate.expire(key, timeout);
    }

    public Object getHashValue(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }
    
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> getHashEntries(String key) {
        return (java.util.Map<String, Object>) (java.util.Map<?, ?>) redisTemplate.opsForHash().entries(key);
    }

    public void deleteHashKey(String key, String hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    public int getHashSize(String key) {
        return redisTemplate.opsForHash().size(key).intValue();
    }
    
    /**
     * SCAN을 사용하여 패턴 매칭 키 조회 (성능 최적화)
     * keys() 명령어의 대안으로 프로덕션 환경에서 안전
     */
    public Set<String> scanKeys(String pattern) {
        return scanKeys(pattern, 1000); // 기본 count: 1000
    }
    
    /**
     * SCAN을 사용하여 패턴 매칭 키 조회 (커스텀 count)
     */
    public Set<String> scanKeys(String pattern, long count) {
        Set<String> keys = new HashSet<>();
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(count)
                    .build();
            
            Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .scan(options);
                    
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
            cursor.close();
        } catch (Exception e) {
            // SCAN 실패 시 fallback으로 keys() 사용 (개발 환경에서만)
            try {
                Set<String> fallbackKeys = redisTemplate.keys(pattern);
                if (fallbackKeys != null) {
                    keys.addAll(fallbackKeys);
                }
            } catch (Exception fallbackException) {
                // 로그만 남기고 빈 Set 반환
                log.error("Redis SCAN과 keys() 모두 실패", fallbackException);
            }
        }
        return keys;
    }
}