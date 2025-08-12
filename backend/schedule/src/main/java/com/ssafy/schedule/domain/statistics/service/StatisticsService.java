package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.dto.response.UserBasicStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserDetailedStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserStreakStatsResponse;

/**
 * 통계 서비스 인터페이스
 * - 사용자 승률 및 연승 통계 계산
 */
public interface StatisticsService {
    
    /**
     * 사용자 기본 통계 계산 및 캐싱
     * @param userId 사용자 ID
     * @param season 시즌 ("total" 또는 "2025" 등)
     * @param teamId 사용자 응원팀 ID
     * @return 기본 통계 (선택 시즌 승률 등)
     */
    UserBasicStatsResponse calculateUserBasicStats(Long userId, String season, Long teamId);
    
    /**
     * 사용자 상세 통계 계산 및 캐싱
     * @param userId 사용자 ID
     * @param season 시즌 ("total" 또는 "2025" 등)
     * @param teamId 사용자 응원팀 ID
     * @return 상세 통계 (구장별, 상대팀별, 요일별 승률 등)
     */
    UserDetailedStatsResponse calculateUserDetailedStats(Long userId, String season, Long teamId);
    
    /**
     * 사용자 연승 통계 계산 및 캐싱
     * @param userId 사용자 ID
     * @param season 시즌 ("total" 또는 "2025" 등)
     * @param teamId 사용자 응원팀 ID
     * @return 연승 통계 (현재 연승, 최장 연승 등)
     */
    UserStreakStatsResponse calculateUserStreakStats(Long userId, String season, Long teamId);
    
    /**
     * 사용자의 모든 통계 재계산
     * @param userId 사용자 ID
     */
    void recalculateUserStats(Long userId);
}