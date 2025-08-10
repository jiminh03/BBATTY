package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.global.constant.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 랭킹 업데이트 서비스 구현체
 * - 통계 계산 후 랭킹 업데이트 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingUpdateServiceImpl implements RankingUpdateService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void updateUserWinRateRanking(Long userId, Long teamId, double winRate) {
        try {
            // 전체 랭킹 업데이트
            updateGlobalRanking(userId, winRate);
            
            // 팀별 랭킹 업데이트
            if (teamId != null) {
                updateTeamRanking(userId, teamId, winRate);
            }
            
            log.debug("승률 랭킹 업데이트 완료: userId={}, winRate={}, teamId={}", userId, winRate, teamId);
            
        } catch (Exception e) {
            log.error("승률 랭킹 업데이트 실패: userId={}, winRate={}, teamId={}", userId, winRate, teamId, e);
        }
    }
    
    /**
     * 전체 승률 랭킹 업데이트
     */
    private void updateGlobalRanking(Long userId, double winRate) {
        String globalRankingKey = RedisKey.RANKING_GLOBAL_TOP10;
        redisTemplate.opsForZSet().add(globalRankingKey, userId, winRate);
        
        // Top 10만 유지 (11번째부터 끝까지 삭제)
        Long totalCount = redisTemplate.opsForZSet().zCard(globalRankingKey);
        if (totalCount != null && totalCount > 10) {
            redisTemplate.opsForZSet().removeRange(globalRankingKey, 0, totalCount - 11);
        }
        
        log.debug("전체 랭킹 업데이트: userId={}, winRate={}", userId, winRate);
    }
    
    /**
     * 팀별 승률 랭킹 업데이트
     */
    private void updateTeamRanking(Long userId, Long teamId, double winRate) {
        String teamRankingKey = RedisKey.RANKING_TEAM_TOP10 + teamId + ":top10";
        redisTemplate.opsForZSet().add(teamRankingKey, userId, winRate);
        
        // Top 10만 유지
        Long teamTotalCount = redisTemplate.opsForZSet().zCard(teamRankingKey);
        if (teamTotalCount != null && teamTotalCount > 10) {
            redisTemplate.opsForZSet().removeRange(teamRankingKey, 0, teamTotalCount - 11);
        }
        
        log.debug("팀별 랭킹 업데이트: userId={}, teamId={}, winRate={}", userId, teamId, winRate);
    }
}