package com.ssafy.bbatty.domain.team.service;

import com.ssafy.bbatty.domain.team.dto.response.TeamRankingResponseDto;

import java.util.List;

public interface TeamService {
    List<TeamRankingResponseDto> getTeamRanking();
}
