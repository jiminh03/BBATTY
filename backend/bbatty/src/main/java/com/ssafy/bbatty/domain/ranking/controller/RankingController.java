package com.ssafy.bbatty.domain.ranking.controller;

import com.ssafy.bbatty.domain.ranking.dto.response.GlobalRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.TeamRankingResponse;
import com.ssafy.bbatty.domain.ranking.service.RankingService;
import com.ssafy.bbatty.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {
    
    private final RankingService rankingService;
    
    /**
     * 전체 승률 랭킹 조회 (TOP 10)
     * GET /api/ranking/global
     */
    @GetMapping("/global")
    public ResponseEntity<ApiResponse<GlobalRankingResponse>> getGlobalWinRateRanking() {
        GlobalRankingResponse response = rankingService.getGlobalWinRateRanking();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 팀별 승률 랭킹 조회 (TOP 10)
     * GET /api/ranking/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<TeamRankingResponse>> getTeamWinRateRanking(
            @PathVariable Long teamId) {
        TeamRankingResponse response = rankingService.getTeamWinRateRanking(teamId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}