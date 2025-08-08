package com.ssafy.schedule.domain.statistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 사용자 상세 통계 응답 DTO
 * - 프로필 통계 탭에서 사용되는 세부 통계 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailedStatsResponse {
    
    private Long userId;
    private String season; // "all" 또는 "2025" 등
    
    // 기본 승무패
    private int totalGames;
    private int wins;
    private int draws;
    private int losses;
    private String winRate; // "0.000"
    
    // 구장별 승률 (구장명 -> 승률)
    private Map<String, CategoryStats> stadiumStats;
    
    // 상대팀별 승률 (팀명 -> 승률)
    private Map<String, CategoryStats> opponentStats;
    
    // 요일별 승률 (MONDAY, TUESDAY... -> 승률)
    private Map<String, CategoryStats> dayOfWeekStats;
    
    // 홈/원정별 승률
    private CategoryStats homeStats; // 홈경기 (응원팀이 홈팀)
    private CategoryStats awayStats; // 원정경기 (응원팀이 원정팀)
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private int games;
        private int wins;
        private int draws;
        private int losses;
        private String winRate; // "0.000"
        
        public void updateWinRate() {
            int decisiveGames = wins + losses;
            if (decisiveGames == 0) {
                this.winRate = "0.000";
            } else {
                double rate = (double) wins / decisiveGames;
                this.winRate = String.format("%.3f", rate);
            }
        }
        
        public static CategoryStats empty() {
            return CategoryStats.builder()
                    .games(0)
                    .wins(0)
                    .draws(0)
                    .losses(0)
                    .winRate("0.000")
                    .build();
        }
    }
    
    public void updateWinRate() {
        int decisiveGames = wins + losses;
        if (decisiveGames == 0) {
            this.winRate = "0.000";
        } else {
            double rate = (double) wins / decisiveGames;
            this.winRate = String.format("%.3f", rate);
        }
    }
}