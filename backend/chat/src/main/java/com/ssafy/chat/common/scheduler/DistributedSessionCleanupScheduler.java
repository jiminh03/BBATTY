package com.ssafy.chat.common.scheduler;

import com.ssafy.chat.common.service.DistributedSessionManagerService;
import com.ssafy.chat.config.ChatConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Map;

/**
 * 분산 세션 정리 스케줄러
 * 비활성 세션 정리 및 모니터링
 */
@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
@EnableAsync
public class DistributedSessionCleanupScheduler {
    
    private final DistributedSessionManagerService distributedSessionManager;
    private final ChatConfiguration chatConfiguration;
    
    /**
     * 애플리케이션 시작 시 이전 인스턴스의 세션들 정리
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void cleanupPreviousInstanceSessions() {
        try {
            String currentInstanceId = chatConfiguration.getOrGenerateInstanceId();
            log.info("애플리케이션 시작 - 인스턴스 ID: {}", currentInstanceId);
            
            // 잠시 대기 후 이전 세션들 정리
            Thread.sleep(5000);
            
            int cleanedCount = distributedSessionManager.cleanupInstanceSessions(currentInstanceId);
            if (cleanedCount > 0) {
                log.info("이전 인스턴스 세션 정리 완료 - 정리된 세션 수: {}", cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("이전 인스턴스 세션 정리 실패", e);
        }
    }
    
    /**
     * 비활성 세션 정리 (30초마다)
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    public void cleanupInactiveSessions() {
        try {
            int cleanedCount = distributedSessionManager.cleanupInactiveSessions();
            
            if (cleanedCount > 0) {
                log.info("비활성 세션 정리 완료 - 정리된 세션 수: {}", cleanedCount);
            } else {
                log.debug("비활성 세션 정리 - 정리할 세션 없음");
            }
            
        } catch (Exception e) {
            log.error("비활성 세션 정리 중 오류 발생", e);
        }
    }
    
    /**
     * 세션 통계 로깅 (5분마다)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    public void logSessionStatistics() {
        try {
            int totalSessions = distributedSessionManager.getTotalActiveSessionCount();
            int totalRooms = distributedSessionManager.getTotalActiveRoomCount();
            Map<String, Integer> instanceDistribution = distributedSessionManager.getSessionDistributionByInstance();
            
            log.info("📊 분산 세션 통계 - 전체 세션: {}, 전체 채팅방: {}, 인스턴스 분포: {}", 
                    totalSessions, totalRooms, instanceDistribution);
            
            // 현재 인스턴스의 상세 통계
            String currentInstanceId = chatConfiguration.getOrGenerateInstanceId();
            int currentInstanceSessions = instanceDistribution.getOrDefault(currentInstanceId, 0);
            
            log.info("🏠 현재 인스턴스 통계 - instanceId: {}, 세션 수: {}", 
                    currentInstanceId, currentInstanceSessions);
            
            // 임계값 체크
            if (totalSessions > chatConfiguration.getMaxConcurrentSessions()) {
                log.warn("⚠️ 전체 세션 수가 임계값 초과 - 현재: {}, 임계값: {}", 
                        totalSessions, chatConfiguration.getMaxConcurrentSessions());
            }
            
        } catch (Exception e) {
            log.error("세션 통계 로깅 실패", e);
        }
    }
    
    /**
     * 세션 건강성 체크 (1분마다)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 120000)
    public void performHealthCheck() {
        try {
            String currentInstanceId = chatConfiguration.getOrGenerateInstanceId();
            Map<String, Integer> distribution = distributedSessionManager.getSessionDistributionByInstance();
            
            // 다른 인스턴스들의 세션 분포 체크
            boolean hasUnbalance = false;
            int maxSessions = 0;
            int minSessions = Integer.MAX_VALUE;
            
            for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                int sessionCount = entry.getValue();
                maxSessions = Math.max(maxSessions, sessionCount);
                minSessions = Math.min(minSessions, sessionCount);
            }
            
            // 인스턴스 간 세션 수 차이가 너무 큰 경우 경고
            if (distribution.size() > 1 && (maxSessions - minSessions) > 1000) {
                hasUnbalance = true;
                log.warn("⚠️ 인스턴스 간 세션 불균형 감지 - 최대: {}, 최소: {}, 차이: {}", 
                        maxSessions, minSessions, (maxSessions - minSessions));
            }
            
            if (!hasUnbalance) {
                log.debug("✅ 분산 세션 건강성 체크 완료 - 정상");
            }
            
        } catch (Exception e) {
            log.error("세션 건강성 체크 실패", e);
        }
    }
    
    /**
     * 애플리케이션 종료 시 현재 인스턴스의 모든 세션 정리
     */
    @PreDestroy
    public void cleanupCurrentInstanceSessions() {
        try {
            String currentInstanceId = chatConfiguration.getOrGenerateInstanceId();
            log.info("애플리케이션 종료 - 현재 인스턴스 세션 정리 시작: {}", currentInstanceId);
            
            int cleanedCount = distributedSessionManager.cleanupInstanceSessions(currentInstanceId);
            
            log.info("애플리케이션 종료 - 세션 정리 완료: 정리된 세션 수 {}", cleanedCount);
            
        } catch (Exception e) {
            log.error("애플리케이션 종료 시 세션 정리 실패", e);
        }
    }
}