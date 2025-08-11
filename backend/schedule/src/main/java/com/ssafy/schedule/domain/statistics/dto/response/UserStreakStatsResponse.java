package com.ssafy.schedule.domain.statistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 사용자 연승 통계 응답 DTO
 * - 현재 연승, 최장 연승 기록 관련 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreakStatsResponse {
    
    private Long userId;
    private String currentSeason; // "2025"
    
    // 현재 연승 기록
    private int currentWinStreak; // 현재 연승 중인 경기 수
    
    // 통산 최장 연승 기록
    private int maxWinStreakAll; // 통산 최장 연승
    
    // 현재 시즌 최장 연승
    private int maxWinStreakCurrentSeason; // 이번 시즌 최장 연승
    
    // 시즌별 최장 연승 기록 (시즌 -> 최장 연승)
    private Map<String, Integer> maxWinStreakBySeason; // "2024" -> 15, "2023" -> 8 등
    
    // 선택 시즌 승무패 정보 (시즌별 또는 통산)
    private int totalGames; // 총 직관 경기 수
    private int wins; // 승수
    private int draws; // 무승부 수
    private int losses; // 패수
    
    public static UserStreakStatsResponse empty(Long userId, String currentSeason) {
        return UserStreakStatsResponse.builder()
                .userId(userId)
                .currentSeason(currentSeason)
                .currentWinStreak(0)
                .maxWinStreakAll(0)
                .maxWinStreakCurrentSeason(0)
                .maxWinStreakBySeason(Map.of())
                .build();
    }
}