package com.ssafy.schedule.service;

import com.ssafy.schedule.common.GameStatus;
import com.ssafy.schedule.entity.Game;
import com.ssafy.schedule.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 애플리케이션 시작 시 기존 예정된 경기들의 이벤트 스케줄을 재등록하는 서비스
 * - 서버 재시작 후에도 경기 시작 2시간 전 이벤트가 정상 작동하도록 보장
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameEventStartupService implements ApplicationRunner {

    private final GameRepository gameRepository;
    private final GameEventScheduler gameEventScheduler;

    /**
     * 애플리케이션 시작 시 실행되는 메서드
     * - 기존 예정된 경기들의 이벤트 스케줄을 재등록
     * 
     * @param args 애플리케이션 실행 인자
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("🚀 애플리케이션 시작: 기존 예정된 경기들의 이벤트 스케줄 재등록 시작");
        
        try {
            rescheduleExistingGames();
        } catch (Exception e) {
            log.error("기존 경기 이벤트 스케줄 재등록 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 기존 예정된 경기들의 이벤트 스케줄 재등록
     * - SCHEDULED 상태이면서 현재 시간 이후인 경기들만 처리
     */
    private void rescheduleExistingGames() {
        LocalDateTime now = LocalDateTime.now();
        
        // 현재 시간 이후의 예정된 경기들 조회
        List<Game> scheduledGames = gameRepository.findByStatusAndDateTimeAfter(GameStatus.SCHEDULED, now);
        
        if (scheduledGames.isEmpty()) {
            log.info("재등록할 예정된 경기가 없습니다.");
            return;
        }

        int scheduledCount = 0;
        int skippedCount = 0;

        for (Game game : scheduledGames) {
            try {
                // 경기 시작 2시간 전이 이미 지났으면 스케줄하지 않음
                LocalDateTime eventTime = game.getDateTime().minusHours(2);
                if (eventTime.isBefore(now) || eventTime.isEqual(now)) {
                    log.debug("경기 ID {} 이벤트 시간이 이미 지남 - 스케줄 건너뛰기: {} vs {} at {}", 
                            game.getId(), 
                            game.getAwayTeam().getName(), 
                            game.getHomeTeam().getName(), 
                            game.getDateTime());
                    skippedCount++;
                    continue;
                }

                // 이벤트 스케줄 등록
                boolean scheduled = gameEventScheduler.scheduleGameStartingEvent(game);
                if (scheduled) {
                    scheduledCount++;
                } else {
                    skippedCount++;
                }
                
            } catch (Exception e) {
                log.warn("경기 ID {} 이벤트 스케줄 재등록 실패: {}", game.getId(), e.getMessage());
                skippedCount++;
            }
        }

        log.info("✅ 기존 경기 이벤트 스케줄 재등록 완료: {}개 등록, {}개 건너뛰기, 총 {}개 경기 처리", 
                scheduledCount, skippedCount, scheduledGames.size());
        
        // 현재 스케줄된 작업 수 로깅
        int currentScheduledTasks = gameEventScheduler.getScheduledTaskCount();
        log.info("현재 활성 이벤트 스케줄: {}개", currentScheduledTasks);
        
        // 서버 시작 시 모든 스케줄된 이벤트 목록 출력
        gameEventScheduler.printAllScheduledTasks();
    }
}