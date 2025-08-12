package com.ssafy.bbatty.domain.ranking.service;

import com.ssafy.bbatty.domain.ranking.dto.response.GlobalRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.TeamRankingResponse;

public interface RankingService {
    
    /**
     * 전체 승률 랭킹 조회 (TOP 10 + 내 순위)
     * @param currentUserId 현재 사용자 ID
     * @return 전체 승률 랭킹 + 내 순위
     */
    GlobalRankingResponse getGlobalWinRateRankingWithMyRank(Long currentUserId);
    
    /**
     * 팀별 승률 랭킹 조회 (TOP 10 + 내 순위)
     * @param teamId 팀 ID
     * @param currentUserId 현재 사용자 ID
     * @param currentUserTeamId 현재 사용자의 팀 ID
     * @return 팀별 승률 랭킹 + 내 순위
     */
    TeamRankingResponse getTeamWinRateRankingWithMyRank(Long teamId, Long currentUserId, Long currentUserTeamId);
}