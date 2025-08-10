package com.ssafy.bbatty.domain.team.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeamRankingResponseDto {
    private String teamName;
    private int games;
    private int wins;
    private int draws;
    private int losses;
    private double winRate;
    private double gameBehind;
    private int streak;
    
    public String getStreakText() {
        if (streak > 0) {
            return streak + "연승";
        } else if (streak < 0) {
            return Math.abs(streak) + "연패";
        } else {
            return "-";
        }
    }
}