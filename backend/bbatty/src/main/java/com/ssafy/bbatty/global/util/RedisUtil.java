package com.ssafy.bbatty.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

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
    
    /**
     * TypeReference를 사용한 제네릭 타입 안전 조회
     */
    public <T> T getValue(String key, TypeReference<T> typeReference) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Redis 값 변환 실패: " + e.getMessage(), e);
        }
    }

    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 키에 TTL 설정
     */
    public void expire(String key, Duration timeout) {
        redisTemplate.expire(key, timeout);
    }

    // ===========================================
    // SET 관련 메서드들
    // ===========================================

    /**
     * Set에 값 추가
     */
    public void addToSet(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
    }
    
    
    // ===========================================
    // HASH 관련 메서드들
    // ===========================================
    
    /**
     * Hash에 필드-값 저장
     */
    public void putToHash(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }
    
    /**
     * Hash의 모든 필드와 값 조회
     */
    public java.util.Map<String, String> getHashAll(String key) {
        return redisTemplate.opsForHash().entries(key).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()),
                    entry -> String.valueOf(entry.getValue())
                ));
    }
    
    
    // ===========================================
    // SORTED SET 관련 메서드들
    // ===========================================
    
    /**
     * Sorted Set에서 역순으로 범위 조회 (최신순)
     */
    public java.util.Set<String> reverseRange(String key, long start, long end) {
        return Objects.requireNonNull(redisTemplate.opsForZSet().reverseRange(key, start, end)).stream()
                .map(String::valueOf)
                .collect(java.util.LinkedHashSet::new, java.util.Set::add, java.util.Set::addAll);
    }
    
    /**
     * Sorted Set에서 스코어 기준 역순 범위 조회
     */
    public java.util.Set<String> reverseRangeByScore(String key, double min, double max, long count) {
        return Objects.requireNonNull(redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, 0, count)).stream()
                .map(String::valueOf)
                .collect(java.util.LinkedHashSet::new, java.util.Set::add, java.util.Set::addAll);
    }
    
    /**
     * Sorted Set에서 멤버의 스코어 조회
     */
    public Long getScore(String key, String member) {
        Double score = redisTemplate.opsForZSet().score(key, member);
        return score != null ? score.longValue() : null;
    }
    
    /**
     * Sorted Set에 값 추가
     */
    public void addToSortedSet(String key, String value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }
}