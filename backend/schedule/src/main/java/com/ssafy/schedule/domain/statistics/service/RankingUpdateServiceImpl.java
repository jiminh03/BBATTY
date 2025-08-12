package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.global.constants.RedisKey;
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
    
    private static final int TOP_RANKING_COUNT = 10;
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void updateUserWinRateRanking(Long userId, Long teamId, double winRate) {
        if (userId == null) {
            log.warn("사용자 ID가 null이어서 랭킹 업데이트 중단");
            return;
        }
        
        if (winRate < 0.0 || winRate > 1.0) {
            log.warn("비정상적인 승률 값: userId={}, winRate={}", userId, winRate);
            return;
        }
        
        boolean globalUpdated = false;
        boolean teamUpdated = false;
        
        try {
            // 전체 랭킹 업데이트 (TOP 10 + 전체 랭킹)
            updateGlobalRanking(userId, winRate);
            globalUpdated = true;
            
        } catch (Exception e) {
            log.error("전체 랭킹 업데이트 실패: userId={}, winRate={}", userId, winRate, e);
        }
        
        try {
            // 팀별 랭킹 업데이트 (TOP 10 + 전체 팀 랭킹)
            if (teamId != null) {
                updateTeamRanking(userId, teamId, winRate);
                teamUpdated = true;
            }
            
        } catch (Exception e) {
            log.error("팀별 랭킹 업데이트 실패: userId={}, teamId={}, winRate={}", userId, teamId, winRate, e);
        }
        
        log.debug("랭킹 업데이트 완료: userId={}, global={}, team={}", userId, globalUpdated, teamUpdated);
    }
    
    /**
     * 전체 승률 랭킹 업데이트
     */
    private void updateGlobalRanking(Long userId, double winRate) {
        try {
            String globalRankingKey = RedisKey.RANKING_GLOBAL_TOP10;
            String globalAllRankingKey = "ranking:global:all";
            
            // Redis 연결 상태 및 키 유효성 검증
            if (globalRankingKey == null || globalRankingKey.trim().isEmpty()) {
                throw new IllegalStateException("전체 랭킹 Redis 키가 비어있음");
            }
            
            // TOP 10 랭킹에 추가
            redisTemplate.opsForZSet().add(globalRankingKey, userId, winRate);
            
            // 전체 사용자 랭킹에 추가 (모든 사용자 포함)
            redisTemplate.opsForZSet().add(globalAllRankingKey, userId, winRate);
            
            // Top 랭킹만 유지 (TOP 10 키에서만)
            Long totalCount = redisTemplate.opsForZSet().zCard(globalRankingKey);
            if (totalCount != null && totalCount > TOP_RANKING_COUNT) {
                long removeCount = redisTemplate.opsForZSet().removeRange(globalRankingKey, 0, totalCount - TOP_RANKING_COUNT - 1);
                log.debug("전체 랭킹 TOP10 정리: {}+개 제거", removeCount);
            }
            
            log.debug("전체 랭킹 업데이트 성공: userId={}, winRate={}", userId, winRate);
            
        } catch (Exception e) {
            log.error("전체 랭킹 Redis 작업 실패: userId={}, winRate={}", userId, winRate, e);
            throw e;
        }
    }
    
    /**
     * 팀별 승률 랭킹 업데이트
     */
    private void updateTeamRanking(Long userId, Long teamId, double winRate) {
        try {
            if (teamId <= 0) {
                throw new IllegalArgumentException("비정상적인 팀 ID: " + teamId);
            }
            
            String teamRankingKey = RedisKey.RANKING_TEAM_TOP10 + teamId + ":top10";
            String teamAllRankingKey = "ranking:team:" + teamId + ":all";
            
            // TOP 10 팀별 랭킹에 추가
            redisTemplate.opsForZSet().add(teamRankingKey, userId, winRate);
            
            // 전체 팀원 랭킹에 추가 (모든 팀원 포함)
            redisTemplate.opsForZSet().add(teamAllRankingKey, userId, winRate);
            
            // Top 랭킹만 유지 (TOP 10 키에서만)
            Long teamTotalCount = redisTemplate.opsForZSet().zCard(teamRankingKey);
            if (teamTotalCount != null && teamTotalCount > TOP_RANKING_COUNT) {
                long removeCount = redisTemplate.opsForZSet().removeRange(teamRankingKey, 0, teamTotalCount - TOP_RANKING_COUNT - 1);
                log.debug("팀별 랭킹 TOP10 정리: teamId={}, {}+개 제거", teamId, removeCount);
            }
            
            log.debug("팀별 랭킹 업데이트 성공: userId={}, teamId={}, winRate={}", userId, teamId, winRate);
            
        } catch (Exception e) {
            log.error("팀별 랭킹 Redis 작업 실패: userId={}, teamId={}, winRate={}", userId, teamId, winRate, e);
            throw e;
        }
    }
}