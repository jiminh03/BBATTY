package com.ssafy.schedule.domain.statistics.service;

/**
 * 랭킹 업데이트 서비스 인터페이스
 * - 통계 계산 후 랭킹 업데이트 담당
 */
public interface RankingUpdateService {
    
    /**
     * 사용자 승률 랭킹 업데이트 (전체 + 팀별)
     * 
     * @param userId 사용자 ID
     * @param teamId 팀 ID (null 가능)
     * @param winRate 승률
     */
    void updateUserWinRateRanking(Long userId, Long teamId, double winRate);
}