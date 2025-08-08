package com.ssafy.schedule.domain.statistics.repository;

import com.ssafy.schedule.global.constant.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 통계 관련 Redis Repository
 * - 계산된 통계 데이터 저장/조회
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class StatisticsRedisRepository {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // ===========================================
    // 통계 데이터 저장/조회 메서드들
    // ===========================================
    
    /**
     * 사용자 기본 승률 통계 저장
     */
    public void saveUserWinRate(Long userId, String season, Object winRateStats) {
        String key = RedisKey.STATS_USER_WINRATE + userId + ":" + season;
        redisTemplate.opsForValue().set(key, winRateStats, Duration.ofHours(24));
        log.debug("사용자 승률 통계 저장: userId={}, season={}", userId, season);
    }
    
    /**
     * 사용자 기본 승률 통계 조회
     */
    public Object getUserWinRate(Long userId, String season) {
        String key = RedisKey.STATS_USER_WINRATE + userId + ":" + season;
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 사용자 연승 통계 저장
     */
    public void saveUserStreakStats(Long userId, Object streakStats) {
        String key = RedisKey.STATS_USER_STREAK + userId;
        redisTemplate.opsForValue().set(key, streakStats, Duration.ofDays(30));
        log.debug("사용자 연승 통계 저장: userId={}", userId);
    }
    
    /**
     * 사용자 연승 통계 조회
     */
    public Object getUserStreakStats(Long userId) {
        String key = RedisKey.STATS_USER_STREAK + userId;
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 사용자 상세 통계 저장 (시즌별)
     */
    public void saveUserDetailedStats(Long userId, String season, Object detailedStats) {
        String key = RedisKey.STATS_USER_DETAILED + userId + ":" + season;
        redisTemplate.opsForValue().set(key, detailedStats, Duration.ofHours(12));
        log.debug("사용자 상세 통계 저장: userId={}, season={}", userId, season);
    }
    
    /**
     * 사용자 상세 통계 조회 (시즌별)
     */
    public Object getUserDetailedStats(Long userId, String season) {
        String key = RedisKey.STATS_USER_DETAILED + userId + ":" + season;
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 사용자의 모든 통계 데이터 삭제 (재계산 시)
     */
    public void clearUserStats(Long userId) {
        String winRateKey = RedisKey.STATS_USER_WINRATE + userId;
        String streakStatsKey = RedisKey.STATS_USER_STREAK + userId;
        
        redisTemplate.delete(winRateKey);
        redisTemplate.delete(streakStatsKey);
        
        log.info("사용자 통계 캐시 삭제: userId={}", userId);
    }
}