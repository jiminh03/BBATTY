package com.ssafy.schedule.domain.bestpost.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularPostRankingService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String POPULAR_POST_KEY_PREFIX = "popular_posts:team:";
    private static final int MAX_STORAGE_SIZE = 100; // 저장소는 100개 유지
    private static final Duration TTL = Duration.ofDays(3); // 3일 TTL
    
    /**
     * 팀별 인기글 점수 업데이트 (기존 점수에 추가)
     */
    public void updatePostScore(Long teamId, Long postId, double score) {
        String key = getPopularPostKey(teamId);
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        
        // 현재 점수에 추가 (없으면 새로 추가)
        Double currentScore = zSetOps.score(key, postId.toString());
        double newScore = (currentScore != null ? currentScore : 0.0) + score;
        
        zSetOps.add(key, postId.toString(), newScore);
        
        // TTL 설정 (새 데이터 추가시마다 갱신)
        redisTemplate.expire(key, TTL);
        
        // 저장소 크기 유지 (100개)
        maintainStorageSize(key);
        
        log.debug("팀 {} 게시글 {} 점수 업데이트: {} -> {}", teamId, postId, currentScore, newScore);
    }
    
    /**
     * 팀별 인기글 점수 설정 (기존 점수 무시하고 새로 설정)
     */
    public void setPostScore(Long teamId, Long postId, double score) {
        String key = getPopularPostKey(teamId);
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        
        zSetOps.add(key, postId.toString(), score);
        redisTemplate.expire(key, TTL);
        maintainStorageSize(key);
        
        log.debug("팀 {} 게시글 {} 점수 설정: {}", teamId, postId, score);
    }
    
    /**
     * 모든 팀의 인기글 점수 감쇠 (30분마다 실행)
     */
    public void applyDecayToAllTeams(double decayRate) {
        Set<String> keys = redisTemplate.keys(POPULAR_POST_KEY_PREFIX + "*");
        
        if (keys != null) {
            for (String key : keys) {
                applyDecayToTeam(key, decayRate);
            }
        }
        
        log.info("모든 팀의 인기글 점수 감쇠 완료. 감쇠율: {}", decayRate);
    }
    
    /**
     * 특정 팀의 인기글 점수 감쇠
     */
    public void applyDecayToTeam(String key, double decayRate) {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        
        Set<ZSetOperations.TypedTuple<Object>> posts = zSetOps.rangeWithScores(key, 0, -1);
        
        if (posts != null && !posts.isEmpty()) {
            for (ZSetOperations.TypedTuple<Object> post : posts) {
                if (post.getValue() != null && post.getScore() != null) {
                    double newScore = post.getScore() * decayRate;
                    
                    // 점수가 너무 낮아지면 제거 (0.1 미만)
                    if (newScore < 0.05) {
                        zSetOps.remove(key, post.getValue());
                    } else {
                        zSetOps.add(key, post.getValue(), newScore);
                    }
                }
            }
        }
    }
    
    /**
     * 저장소 크기 유지 (100개)
     */
    private void maintainStorageSize(String key) {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        Long size = zSetOps.size(key);
        
        if (size != null && size > MAX_STORAGE_SIZE) {
            // 하위 게시글들 제거 (100개만 유지)
            long removeCount = size - MAX_STORAGE_SIZE;
            zSetOps.removeRange(key, 0, removeCount - 1);
            
            log.debug("팀 저장소 크기 조정: {} -> {}", size, MAX_STORAGE_SIZE);
        }
    }
    
    /**
     * 팀별 인기글 Redis 키 생성
     */
    private String getPopularPostKey(Long teamId) {
        return POPULAR_POST_KEY_PREFIX + teamId;
    }
}