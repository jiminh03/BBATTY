package com.ssafy.bbatty.domain.ranking.service;

import com.ssafy.bbatty.domain.ranking.dto.response.GlobalRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.TeamRankingResponse;

public interface RankingService {
    
    /**
     * 전체 승률 랭킹 조회 (TOP 10)
     * @return 전체 승률 랭킹
     */
    GlobalRankingResponse getGlobalWinRateRanking();
    
    /**
     * 팀별 승률 랭킹 조회 (TOP 10)
     * @param teamId 팀 ID
     * @return 팀별 승률 랭킹
     */
    TeamRankingResponse getTeamWinRateRanking(Long teamId);
}