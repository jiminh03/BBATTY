package com.ssafy.schedule.domain.statistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRankingResponse {
    private Long userId;
    private String nickname;
    private Long teamId;
    private Integer totalGames;
    private Integer wins;
    private Integer losses;
    private String winRate;
    private Integer rank;
}