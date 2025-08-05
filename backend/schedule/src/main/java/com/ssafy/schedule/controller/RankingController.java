package com.ssafy.schedule.controller;

import com.ssafy.schedule.service.TeamRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
@Slf4j
public class RankingController {

    private final TeamRankingService teamRankingService;

    @GetMapping("/teams/{year}")
    public ResponseEntity<Map<String, Object>> getTeamRanking(@PathVariable int year) {
        log.info("{}년 구단 순위 조회 요청", year);

        Map<String, Object> response = new HashMap<>();

        try {
            List<TeamRankingService.TeamStats> rankings;

            // 2025년인 경우 Redis 캐시 사용, 다른 연도는 실시간 계산
            if (year == 2025) {
                rankings = teamRankingService.getCurrentRanking();
                log.info("2025년 순위 - Redis 캐시 사용");
            } else {
                rankings = teamRankingService.calculateTeamRanking();
                log.info("{}년 순위 - 실시간 계산", year);
            }

            response.put("success", true);
            response.put("year", year);
            response.put("rankings", rankings);
            response.put("totalTeams", rankings.size());

            // 순위 정보 로그 출력
            log.info("=== {}년 KBO 구단 순위 ===", year);
            for (int i = 0; i < rankings.size(); i++) {
                TeamRankingService.TeamStats stats = rankings.get(i);
                log.info("{}위: {}", i + 1, stats.toString());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("구단 순위 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "순위 조회 실패: " + e.getMessage());
            response.put("year", year);

            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/teams/current")
    public ResponseEntity<Map<String, Object>> getCurrentTeamRanking() {
        int currentYear = LocalDate.now().getYear();
        log.info("현재 연도({}) 구단 순위 조회 요청", currentYear);
        
        return getTeamRanking(currentYear);
    }

    @GetMapping("/teams/2025/until/{date}")
    public ResponseEntity<Map<String, Object>> getTeamRankingUntilDate(@PathVariable String date) {
        log.info("2025년 {}까지 구단 순위 조회 요청", date);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            LocalDate endDate = LocalDate.parse(date);
            List<TeamRankingService.TeamStats> rankings = teamRankingService.calculateTeamRankingUntilDate(endDate);
            
            response.put("success", true);
            response.put("year", 2025);
            response.put("endDate", date);
            response.put("rankings", rankings);
            response.put("totalTeams", rankings.size());
            
            // 순위 정보 로그 출력
            log.info("=== 2025년 {}까지 KBO 구단 순위 ===", date);
            for (int i = 0; i < rankings.size(); i++) {
                TeamRankingService.TeamStats stats = rankings.get(i);
                log.info("{}위: {}", i + 1, stats.toString());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("구단 순위 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "순위 조회 실패: " + e.getMessage());
            response.put("year", 2025);
            response.put("endDate", date);
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}