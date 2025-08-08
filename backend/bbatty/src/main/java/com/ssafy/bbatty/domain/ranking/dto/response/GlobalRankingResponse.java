package com.ssafy.bbatty.domain.ranking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GlobalRankingResponse {
    private String season;
    private List<UserRankingDto> rankings;
}