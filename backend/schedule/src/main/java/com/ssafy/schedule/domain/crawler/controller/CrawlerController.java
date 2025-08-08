package com.ssafy.schedule.domain.crawler.controller;

import com.ssafy.schedule.domain.chat.kafka.ChatEventKafkaProducer;
import com.ssafy.schedule.domain.crawler.scheduler.ChatCreateScheduler;
import com.ssafy.schedule.domain.crawler.service.GameEventService;
import com.ssafy.schedule.domain.crawler.service.ScheduledGameService;
import com.ssafy.schedule.domain.crawler.service.FinishedGameService;
import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.global.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
@Slf4j
public class CrawlerController {

    private final ScheduledGameService scheduledGameService;
    private final FinishedGameService finishedGameService;
    private final ChatEventKafkaProducer chatEventKafkaProducer;
    private final ChatCreateScheduler chatCreateScheduler;
    private final GameEventService gameEventService;
    private final GameRepository gameRepository;

    @PostMapping("/scheduled-games/{date}")
    public ResponseEntity<Map<String, Object>> crawlScheduledGames(@PathVariable String date) {
        log.info("경기 일정 크롤링 요청: {}", date);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 날짜 형식 검증
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 일정 크롤링 및 저장 실행
            List<Game> savedGames = scheduledGameService.crawlAndSaveScheduledGames(date);

            response.put("success", true);
            response.put("message", date + " 경기 일정 크롤링 완료");
            response.put("date", date);
            response.put("games", savedGames);
            
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

    @PostMapping("/past-finished-games/month/{yearMonth}")
    public ResponseEntity<Map<String, Object>> saveFinishedGamesForMonth(@PathVariable String yearMonth) {
        log.info("특정 월 진행된 경기 직접 저장 요청: {}", yearMonth);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 년월 형식 검증 (yyyy-MM)
            YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
            
            int totalSavedCount = 0;
            LocalDate startDate = ym.atDay(1);
            LocalDate endDate = ym.atEndOfMonth();
            
            log.info("{} 전체 날짜 크롤링 시작: {} ~ {}", yearMonth, startDate, endDate);
            
            // 해당 월의 모든 날짜에 대해 크롤링 실행
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                try {
                    int dailySavedCount = finishedGameService.crawlAndSaveFinishedGames(dateStr);
                    totalSavedCount += dailySavedCount;
                    
                    if (dailySavedCount > 0) {
                        log.info("{}일: {}개 경기 저장", dateStr, dailySavedCount);
                    }
                    
                    // 서버 부하 방지를 위한 짧은 대기
                    Thread.sleep(3000);
                    
                } catch (Exception e) {
                    log.warn("{}일 크롤링 실패: {}", dateStr, e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", yearMonth + " 전체 월 진행된 경기 직접 저장 완료");
            response.put("yearMonth", yearMonth);
            response.put("totalSavedCount", totalSavedCount);
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("월별 진행된 경기 직접 저장 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "월별 저장 실패: " + e.getMessage());
            response.put("yearMonth", yearMonth);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ========== 개발용 API ==========
    
    /**
     * 현재 스케줄된 모든 작업 정보 조회 (개발용)
     */
    @GetMapping("/dev/scheduled-tasks")
    public ResponseEntity<Map<String, Object>> getScheduledTasks() {
        log.info("스케줄된 작업 조회 요청 (개발용)");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            int taskCount = chatCreateScheduler.getScheduledTaskCount();
            chatCreateScheduler.printAllScheduledTasks();
            
            response.put("success", true);
            response.put("scheduledTaskCount", taskCount);
            response.put("message", "스케줄된 작업 정보가 로그에 출력되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("스케줄된 작업 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "작업 조회 실패: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 특정 경기의 이벤트를 수동으로 테스트 (개발용)
     */
    @PostMapping("/dev/test-game-event/{gameId}")
    public ResponseEntity<Map<String, Object>> testGameEvent(@PathVariable Long gameId) {
        log.info("경기 이벤트 수동 테스트 요청 (개발용): 경기 ID = {}", gameId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다: " + gameId));
            
            log.info("수동 이벤트 테스트 실행: {} vs {} at {}", 
                    game.getAwayTeam().getName(), 
                    game.getHomeTeam().getName(), 
                    game.getDateTime());
            
            // 이벤트 직접 실행
            gameEventService.handleGameStartingSoonEvent(game);
            
            response.put("success", true);
            response.put("gameId", gameId);
            response.put("message", "경기 이벤트가 수동으로 실행되었습니다.");
            response.put("gameInfo", String.format("%s vs %s", 
                    game.getAwayTeam().getName(), game.getHomeTeam().getName()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("경기 이벤트 수동 테스트 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("gameId", gameId);
            response.put("message", "이벤트 테스트 실패: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}