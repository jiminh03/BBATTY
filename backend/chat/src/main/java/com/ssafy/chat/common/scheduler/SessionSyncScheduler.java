package com.ssafy.chat.common.scheduler;

import com.ssafy.chat.common.service.SessionSyncService;
import com.ssafy.chat.config.ChatConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 세션-사용자 동기화 스케줄러
 * 주기적으로 WebSocket 세션과 Redis 사용자 정보의 일관성을 유지
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionSyncScheduler {
    
    private final SessionSyncService sessionSyncService;
    private final ChatConfiguration chatConfiguration;
    
    /**
     * 전체 채팅방 세션 동기화 (5분마다)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void syncAllRoomSessions() {
        try {
            log.debug("전체 채팅방 세션 동기화 시작");
            
            Map<String, Integer> syncResults = sessionSyncService.syncAllRoomSessions();
            
            int totalCleaned = syncResults.values().stream()
                    .filter(count -> count >= 0) // 실패(-1) 제외
                    .mapToInt(Integer::intValue)
                    .sum();
            
            int failedRooms = (int) syncResults.values().stream()
                    .filter(count -> count < 0)
                    .count();
            
            if (totalCleaned > 0 || failedRooms > 0) {
                log.info("전체 채팅방 세션 동기화 완료 - 정리된 항목: {}, 실패한 채팅방: {}", 
                        totalCleaned, failedRooms);
            } else {
                log.debug("전체 채팅방 세션 동기화 완료 - 정리할 항목 없음");
            }
            
        } catch (Exception e) {
            log.error("전체 채팅방 세션 동기화 스케줄러 실행 실패", e);
        }
    }
    
    /**
     * 동기화 상태 모니터링 및 로깅 (10분마다)
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 300000)
    public void monitorSyncStatus() {
        try {
            SessionSyncService.SyncStatistics stats = sessionSyncService.getGlobalSyncStatistics();
            
            log.info("📊 세션 동기화 상태 모니터링 - " +
                    "전체 채팅방: {}, 동기화됨: {}, 불일치: {}, " +
                    "좀비 세션: {}, 고아 사용자: {}, 동기화율: {:.1f}%",
                    stats.getTotalRooms(), stats.getSyncedRooms(), stats.getUnsyncedRooms(),
                    stats.getTotalZombieSessions(), stats.getTotalOrphanedUsers(),
                    stats.getSyncedRatio() * 100);
            
            // 동기화율이 낮은 경우 경고
            if (stats.getSyncedRatio() < 0.8 && stats.getTotalRooms() > 0) {
                log.warn("⚠️ 세션 동기화율이 낮습니다 - 동기화율: {:.1f}%, " +
                        "좀비 세션: {}, 고아 사용자: {}",
                        stats.getSyncedRatio() * 100,
                        stats.getTotalZombieSessions(),
                        stats.getTotalOrphanedUsers());
            }
            
            // 문제가 많은 경우 상세 상태 로깅
            if (stats.getTotalZombieSessions() + stats.getTotalOrphanedUsers() > 50) {
                logDetailedSyncStatus();
            }
            
        } catch (Exception e) {
            log.error("동기화 상태 모니터링 실패", e);
        }
    }
    
    /**
     * 심각한 불일치가 발생한 채팅방 강제 동기화 (30분마다)
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 900000)
    public void forceSyncProblemRooms() {
        try {
            Map<String, SessionSyncService.SyncStatus> roomStatus = sessionSyncService.getRoomSyncStatus();
            
            int forceSyncCount = 0;
            int successCount = 0;
            
            for (Map.Entry<String, SessionSyncService.SyncStatus> entry : roomStatus.entrySet()) {
                SessionSyncService.SyncStatus status = entry.getValue();
                
                // 심각한 불일치 조건: 불일치가 10개 이상이거나, 좀비/고아가 5개 이상
                boolean needsForceSync = status.getDiscrepancy() >= 10 ||
                                       (status.getZombieSessionCount() + status.getOrphanedUserCount()) >= 5;
                
                if (needsForceSync) {
                    log.warn("심각한 불일치 채팅방 발견 - roomId: {}, 불일치: {}, 좀비: {}, 고아: {}",
                            entry.getKey(), status.getDiscrepancy(),
                            status.getZombieSessionCount(), status.getOrphanedUserCount());
                    
                    SessionSyncService.SyncResult result = sessionSyncService.forceSync(entry.getKey(), true);
                    forceSyncCount++;
                    
                    if (result.isSuccess()) {
                        successCount++;
                        log.info("강제 동기화 성공 - roomId: {}, 정리된 항목: {}",
                                result.getRoomId(), result.getTotalChanges());
                    } else {
                        log.error("강제 동기화 실패 - roomId: {}, 오류: {}",
                                result.getRoomId(), result.getErrorMessage());
                    }
                }
            }
            
            if (forceSyncCount > 0) {
                log.info("문제 채팅방 강제 동기화 완료 - 대상: {}, 성공: {}", forceSyncCount, successCount);
            }
            
        } catch (Exception e) {
            log.error("문제 채팅방 강제 동기화 실패", e);
        }
    }
    
    /**
     * 상세 동기화 상태 로깅
     */
    private void logDetailedSyncStatus() {
        try {
            Map<String, SessionSyncService.SyncStatus> roomStatus = sessionSyncService.getRoomSyncStatus();
            
            log.info("📋 상세 세션 동기화 상태:");
            
            // 가장 문제가 많은 채팅방 10개 로깅
            roomStatus.entrySet().stream()
                    .filter(entry -> !entry.getValue().isSynced())
                    .sorted((e1, e2) -> Integer.compare(
                            e2.getValue().getDiscrepancy() + 
                            e2.getValue().getZombieSessionCount() + 
                            e2.getValue().getOrphanedUserCount(),
                            e1.getValue().getDiscrepancy() + 
                            e1.getValue().getZombieSessionCount() + 
                            e1.getValue().getOrphanedUserCount()))
                    .limit(10)
                    .forEach(entry -> {
                        SessionSyncService.SyncStatus status = entry.getValue();
                        log.info("  🔴 불일치 채팅방 - roomId: {}, Redis: {}, 실제: {}, 좀비: {}, 고아: {}",
                                entry.getKey(),
                                status.getRedisUserCount(),
                                status.getActualSessionCount(),
                                status.getZombieSessionCount(),
                                status.getOrphanedUserCount());
                    });
                    
        } catch (Exception e) {
            log.error("상세 동기화 상태 로깅 실패", e);
        }
    }
}