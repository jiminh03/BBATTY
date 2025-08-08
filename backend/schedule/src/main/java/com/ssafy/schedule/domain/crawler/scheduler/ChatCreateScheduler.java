package com.ssafy.schedule.domain.crawler.scheduler;

import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.domain.crawler.service.GameEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 경기 이벤트 동적 스케줄링 서비스
 * - 경기 시작 2시간 전에 이벤트를 발생시키는 스케줄을 동적으로 관리
 * - TaskScheduler를 사용하여 정확한 시간에 이벤트 실행
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatCreateScheduler {

    @Qualifier("gameEventTaskScheduler")
    private final TaskScheduler taskScheduler;
    private final GameEventService gameEventService;

    /**
     * 스케줄된 작업들을 관리하는 맵
     * Key: 경기 ID, Value: 스케줄된 작업의 Future 객체
     */
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 경기 시작 2시간 전 이벤트 스케줄 등록
     * 
     * @param game 스케줄링할 경기
     * @return 스케줄 등록 성공 여부
     */
    public boolean scheduleGameStartingEvent(Game game) {
        // 이미 스케줄된 경기는 건너뛰기
        if (scheduledTasks.containsKey(game.getId())) {
            log.debug("경기 ID {}는 이미 스케줄되어 있음", game.getId());
            return false;
        }

        // 경기 시간 3시간 전 계산
        LocalDateTime eventTime = game.getDateTime().minusHours(3);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 이미 지난 시간이면 스케줄하지 않음
        if (eventTime.isBefore(now) || eventTime.isEqual(now)) {
            log.debug("경기 ID {} 이벤트 시간이 이미 지났음: {}", game.getId(), eventTime);
            return false;
        }

        try {
            // Date 객체로 변환 (TaskScheduler에서 사용)
            Date scheduledDate = Date.from(eventTime.atZone(ZoneId.systemDefault()).toInstant());
            
            // 스케줄 등록
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                () -> gameEventService.handleGameStartingSoonEvent(game),
                scheduledDate
            );

            // 스케줄된 작업 저장
            scheduledTasks.put(game.getId(), scheduledTask);

            log.info("✅ 경기 이벤트 스케줄 등록: 경기 ID={}, 이벤트 시간={}, 경기={} vs {}", 
                    game.getId(), eventTime, game.getAwayTeam().getName(), game.getHomeTeam().getName());
            
            return true;

        } catch (Exception e) {
            log.error("경기 이벤트 스케줄 등록 실패: 경기 ID={}, 오류={}", game.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 경기 이벤트 스케줄 취소
     * 
     * @param gameId 취소할 경기 ID
     * @return 취소 성공 여부
     */
    public boolean cancelGameEvent(Long gameId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(gameId);
        
        if (scheduledTask != null) {
            boolean cancelled = scheduledTask.cancel(false);
            log.info("경기 이벤트 스케줄 취소: 경기 ID={}, 취소 성공={}", gameId, cancelled);
            return cancelled;
        }
        
        log.debug("취소할 스케줄이 없음: 경기 ID={}", gameId);
        return false;
    }

    /**
     * 현재 스케줄된 작업 수 조회
     * 
     * @return 스케줄된 작업 수
     */
    public int getScheduledTaskCount() {
        return scheduledTasks.size();
    }

    /**
     * 현재 스케줄된 모든 작업 정보를 콘솔에 출력
     */
    public void printAllScheduledTasks() {
        if (scheduledTasks.isEmpty()) {
            log.info("📋 현재 스케줄된 이벤트가 없습니다.");
            return;
        }

        log.info("📋 ========== 현재 스케줄된 이벤트 목록 ({} 개) ==========", scheduledTasks.size());
        
        scheduledTasks.forEach((gameId, task) -> {
            try {
                // 게임 정보 조회 (간단한 로깅을 위해 게임 ID만 출력)
                String status = task.isDone() ? "완료됨" : task.isCancelled() ? "취소됨" : "대기중";
                log.info("🎮 경기 ID: {} | 상태: {} | 작업: {}", gameId, status, task.toString());
            } catch (Exception e) {
                log.warn("경기 ID {} 정보 출력 중 오류: {}", gameId, e.getMessage());
            }
        });
        
        log.info("📋 ========================================================");
    }

    /**
     * 모든 스케줄된 작업 취소 (서버 종료 시 사용)
     */
    public void cancelAllScheduledTasks() {
        log.info("모든 경기 이벤트 스케줄 취소: {}개 작업", scheduledTasks.size());
        
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
    }

    /**
     * 완료된 스케줄 정리 (주기적으로 호출)
     * - 이미 실행 완료되거나 취소된 작업들을 맵에서 제거
     */
    public void cleanupCompletedTasks() {
        int beforeSize = scheduledTasks.size();
        
        scheduledTasks.entrySet().removeIf(entry -> 
            entry.getValue().isDone() || entry.getValue().isCancelled()
        );
        
        int afterSize = scheduledTasks.size();
        int cleanedCount = beforeSize - afterSize;
        
        if (cleanedCount > 0) {
            log.debug("완료된 스케줄 정리: {}개 제거, 현재 {}개 스케줄 활성", cleanedCount, afterSize);
        }
    }
}