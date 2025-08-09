package com.ssafy.bbatty.domain.team.controller;

import com.ssafy.bbatty.domain.team.dto.response.TeamRankingResponseDto;
import com.ssafy.bbatty.domain.team.service.TeamService;
import com.ssafy.bbatty.global.constants.SuccessCode;
import com.ssafy.bbatty.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * 2025년 시즌 팀 순위 조회 API
     * @return 팀 순위 리스트 (승률 기준 정렬)
     */
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<List<TeamRankingResponseDto>>> getTeamRanking() {
        log.info("팀 순위 조회 API 호출");
        
        List<TeamRankingResponseDto> teamRanking = teamService.getTeamRanking();

        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, teamRanking));
    }
}