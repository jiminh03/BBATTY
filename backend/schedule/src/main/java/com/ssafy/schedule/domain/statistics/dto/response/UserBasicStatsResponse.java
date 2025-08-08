package com.ssafy.schedule.domain.statistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 기본 승률 통계 응답 DTO
 * - 앱 전체에서 가장 자주 사용되는 선택 시즌 승률 정보
 * - 승률은 할푼리 형식 (0.000) = 승 / (승 + 패)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicStatsResponse {
    
    private Long userId;
    private String season; // "all", "2025" 등
    
    // 선택 시즌 승무패 정보
    private int totalGames;
    private int wins;
    private int draws;
    private int losses;
    private String winRate; // "0.000" 형식 (할푼리)
    
    // 야구 승률 계산: 승 / (승 + 패) - 무승부 제외
    public static String calculateWinRate(int wins, int losses) {
        int decisiveGames = wins + losses; // 승부가 결정된 경기만
        if (decisiveGames == 0) return "0.000";
        double rate = (double) wins / decisiveGames;
        return String.format("%.3f", rate);
    }
    
    public void updateWinRate() {
        this.winRate = calculateWinRate(this.wins, this.losses);
    }
    
    public static UserBasicStatsResponse empty(Long userId, String season) {
        return UserBasicStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .winRate("0.000")
                .build();
    }
}