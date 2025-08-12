package com.ssafy.bbatty.domain.ranking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRankingDto {
    private Long userId;
    private Double winRate;
    private Integer rank;
    private Double percentile;
    private Boolean isCurrentUser;
}