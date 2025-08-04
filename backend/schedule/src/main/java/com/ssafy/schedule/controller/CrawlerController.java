package com.ssafy.schedule.controller;

import com.ssafy.schedule.repository.GameRepository;
import com.ssafy.schedule.service.ScheduledGameService;
import com.ssafy.schedule.service.FinishedGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
@Slf4j
public class CrawlerController {

    private final ScheduledGameService scheduledGameService;
    private final FinishedGameService finishedGameService;

    @PostMapping("/scheduled-games/{date}")
    public ResponseEntity<Map<String, Object>> crawlScheduledGames(@PathVariable String date) {
        log.info("경기 일정 크롤링 요청: {}", date);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 날짜 형식 검증
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 일정 크롤링 및 저장 실행
            int savedCount = scheduledGameService.crawlAndSaveScheduledGames(date);
            
            response.put("success", true);
            response.put("message", date + " 경기 일정 크롤링 완료");
            response.put("date", date);
            response.put("savedCount", savedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("경기 일정 크롤링 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "크롤링 실패: " + e.getMessage());
            response.put("date", date);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/finished-games/{date}")
    public ResponseEntity<Map<String, Object>> updateFinishedGames(@PathVariable String date) {
        log.info("경기 결과 업데이트 요청: {}", date);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 날짜 형식 검증
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 경기 결과 크롤링 및 업데이트 실행
            int updatedCount = finishedGameService.crawlAndUpdateFinishedGames(date);
            
            response.put("success", true);
            response.put("message", date + " 경기 결과 업데이트 완료");
            response.put("date", date);
            response.put("updatedCount", updatedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("경기 결과 업데이트 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "업데이트 실패: " + e.getMessage());
            response.put("date", date);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/past-finished-games/{date}")
    public ResponseEntity<Map<String, Object>> saveFinishedGames(@PathVariable String date) {
        log.info("진행된 경기 직접 저장 요청: {}", date);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 날짜 형식 검증
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 진행된 경기 크롤링 및 직접 저장 실행
            int savedCount = finishedGameService.crawlAndSaveFinishedGames(date);
            
            response.put("success", true);
            response.put("message", date + " 진행된 경기 직접 저장 완료");
            response.put("date", date);
            response.put("savedCount", savedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("진행된 경기 직접 저장 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "저장 실패: " + e.getMessage());
            response.put("date", date);
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}