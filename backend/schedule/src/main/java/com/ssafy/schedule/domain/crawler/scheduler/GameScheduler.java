package com.ssafy.schedule.domain.crawler.scheduler;

import com.ssafy.schedule.domain.chat.kafka.ChatEventKafkaProducer;
import com.ssafy.schedule.domain.crawler.service.FinishedGameService;
import com.ssafy.schedule.domain.crawler.service.ScheduledGameService;
import com.ssafy.schedule.domain.statistics.service.StatisticsUpdateService;

import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.global.repository.GameRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class GameScheduler {

    private final ScheduledGameService scheduledGameService;
    private final FinishedGameService finishedGameService;
    private final ChatCreateScheduler gameEventScheduler;
    private final StatisticsUpdateService statisticsUpdateService;
    private final GameRepository gameRepository;
    private final ChatEventKafkaProducer chatEventKafkaProducer;


    /**
     * 매일 00:00에 3주뒤의 날짜의 경기 일정을 크롤링하여 저장
     * - 아직 진행되지 않은 예정된 경기들을 SCHEDULED 상태로 저장
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void crawlTodayScheduledGames() {
        
        // 3주 뒤의 날짜 계산
        String targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .plusWeeks(3)  
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        try {
            List<Game> savedGames = scheduledGameService.crawlAndSaveScheduledGames(targetDate);
            log.info("========== ({}) 경기 일정 크롤링 완료: {}개 저장 ==========", targetDate, savedGames.size());

        } catch (Exception e) {
            log.error("========== ({}) 경기 일정 크롤링 실패: {} ==========", targetDate, e.getMessage(), e);
        }
    }

    /**
     * 매일 00:00에 어제 날짜의 경기 결과를 크롤링하여 업데이트
     * - 어제 진행된 경기들의 결과를 기존 일정에 업데이트
     * - 멀티스레드로 동작하므로 경기 일정 크롤링과 동시 실행 가능
     */
    @Scheduled(cron = "0 0 0 * * *",  zone = "Asia/Seoul")
    public void updateYesterdayFinishedGames() {
        String yesterday = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        log.info("========== 어제({}) 경기 결과 업데이트 시작 ==========", yesterday);
        
        try {
            int updatedCount = finishedGameService.crawlAndUpdateFinishedGames(yesterday);
            log.info("========== 어제({}) 경기 결과 업데이트 완료: {}개 업데이트 ==========", yesterday, updatedCount);
            
            // 경기 결과 업데이트 완료 후 관련 사용자 통계 재계산
            statisticsUpdateService.updateUserStatsForDate(yesterday);
            
        } catch (Exception e) {
            log.error("========== 어제({}) 경기 결과 업데이트 실패: {} ==========", yesterday, e.getMessage(), e);
        }
    }

    /**
     * 매일 02:00에 완료된 이벤트 스케줄 정리
     * - 이미 실행 완료되거나 취소된 스케줄 작업들을 메모리에서 제거
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void cleanupCompletedEventSchedules() {
        log.info("========== 완료된 이벤트 스케줄 정리 시작 ==========");
        
        try {
            gameEventScheduler.cleanupCompletedTasks();
            int currentTaskCount = gameEventScheduler.getScheduledTaskCount();
            log.info("========== 이벤트 스케줄 정리 완료: 현재 {}개 스케줄 활성 ==========", currentTaskCount);
        } catch (Exception e) {
            log.error("========== 이벤트 스케줄 정리 실패: {} ==========", e.getMessage(), e);
        }
    }
}