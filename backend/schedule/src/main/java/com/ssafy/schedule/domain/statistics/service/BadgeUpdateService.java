package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.global.constants.Stadium;

/**
 * 뱃지 업데이트 서비스
 */
public interface BadgeUpdateService {
    
    /**
     * 경기 결과 업데이트 시 모든 뱃지 체크 및 업데이트
     */
    void updateBadgesOnGameResult(Long userId, Long gameId, String season);
    
    /**
     * 구장 뱃지 업데이트
     */
    void updateStadiumBadge(Long userId, Stadium stadium);
    
    /**
     * 승리 뱃지 업데이트 
     */
    void updateWinBadges(Long userId, String season, int totalWins);
    
    /**
     * 직관 경기 뱃지 업데이트
     */
    void updateGameBadges(Long userId, String season, int totalGames);
}