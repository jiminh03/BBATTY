package com.ssafy.chat.common.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 채팅 시스템 모니터링 및 에러 처리 유틸리티
 * 
 * 📋 기능:
 * - Redis 연결 실패 감지 및 대응
 * - 세션 정리 실패 추적
 * - 성능 메트릭 수집
 * - 알림 및 로깅 통합
 */
@Slf4j
public class ChatMonitoringUtil {

    // ===========================================
    // 에러 카운터 및 통계
    // ===========================================

    private static final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private static final Map<String, String> lastErrorTimes = new ConcurrentHashMap<>();
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===========================================
    // Redis 에러 처리
    // ===========================================

    /**
     * Redis 연결 실패 처리
     */
    public static void handleRedisConnectionError(String operation, String key, Exception e) {
        String errorType = "REDIS_CONNECTION_FAILED";
        incrementErrorCount(errorType);
        updateLastErrorTime(errorType);
        
        log.error("🔴 Redis 연결 실패 - 작업: {}, 키: {}, 에러: {}", 
                operation, key, e.getMessage(), e);
        
        // 임계치 초과 시 알림 (예: 5번 이상 실패)
        if (getErrorCount(errorType) >= 5) {
            notifyRedisConnectionFailure(operation, key, getErrorCount(errorType));
        }
    }

    /**
     * Redis 키 만료 이벤트 처리
     */
    public static void handleRedisKeyExpired(String key, String keyType) {
        String errorType = "REDIS_KEY_EXPIRED";
        incrementErrorCount(errorType);
        
        log.info("⏰ Redis 키 만료 - 타입: {}, 키: {}", keyType, key);
        
        // 특정 키 타입별 정리 작업
        switch (keyType) {
            case "CHAT_ROOM" -> cleanupExpiredChatRoom(key);
            case "USER_SESSION" -> cleanupExpiredUserSession(key);
            default -> log.debug("알 수 없는 키 타입: {}", keyType);
        }
    }

    /**
     * Redis Scan 성능 모니터링
     */
    public static void monitorScanPerformance(String pattern, int resultCount, long executionTimeMs) {
        if (executionTimeMs > 1000) { // 1초 이상 걸리면 경고
            log.warn("⚡ Redis SCAN 성능 저하 - 패턴: {}, 결과수: {}, 실행시간: {}ms", 
                    pattern, resultCount, executionTimeMs);
        } else {
            log.debug("Redis SCAN 완료 - 패턴: {}, 결과수: {}, 실행시간: {}ms", 
                    pattern, resultCount, executionTimeMs);
        }
    }

    // ===========================================
    // 세션 관리 에러 처리
    // ===========================================

    /**
     * 세션 정리 실패 처리
     */
    public static void handleSessionCleanupError(String sessionId, String roomId, Exception e) {
        String errorType = "SESSION_CLEANUP_FAILED";
        incrementErrorCount(errorType);
        updateLastErrorTime(errorType);
        
        log.error("🧹 세션 정리 실패 - sessionId: {}, roomId: {}, 에러: {}", 
                sessionId, roomId, e.getMessage(), e);
    }

    /**
     * WebSocket 연결 에러 처리
     */
    public static void handleWebSocketError(String sessionId, String errorType, Exception e) {
        String fullErrorType = "WEBSOCKET_" + errorType;
        incrementErrorCount(fullErrorType);
        updateLastErrorTime(fullErrorType);
        
        log.error("🔌 WebSocket 에러 - sessionId: {}, 타입: {}, 에러: {}", 
                sessionId, errorType, e.getMessage(), e);
    }

    // ===========================================
    // TTL 및 만료 관리
    // ===========================================

    /**
     * TTL 계산 오류 처리
     */
    public static void handleTTLCalculationError(String key, String ttlType, Exception e) {
        String errorType = "TTL_CALCULATION_FAILED";
        incrementErrorCount(errorType);
        updateLastErrorTime(errorType);
        
        log.error("⏳ TTL 계산 실패 - 키: {}, TTL타입: {}, 에러: {}", 
                key, ttlType, e.getMessage(), e);
    }

    /**
     * 자정 TTL 경고 (남은 시간이 너무 짧을 때)
     */
    public static void warnShortTTL(String key, long remainingMinutes) {
        if (remainingMinutes < 10) { // 10분 미만 남았을 때
            log.warn("⚠️ TTL 경고 - 키: {}, 남은시간: {}분", key, remainingMinutes);
        }
    }

    // ===========================================
    // 통계 및 메트릭
    // ===========================================

    /**
     * 에러 카운트 증가
     */
    private static void incrementErrorCount(String errorType) {
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 에러 카운트 조회
     */
    public static long getErrorCount(String errorType) {
        AtomicLong count = errorCounts.get(errorType);
        return count != null ? count.get() : 0;
    }

    /**
     * 마지막 에러 시간 업데이트
     */
    private static void updateLastErrorTime(String errorType) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        lastErrorTimes.put(errorType, timestamp);
    }

    /**
     * 전체 에러 통계 조회
     */
    public static Map<String, Object> getErrorStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        errorCounts.forEach((errorType, count) -> {
            Map<String, Object> errorInfo = new ConcurrentHashMap<>();
            errorInfo.put("count", count.get());
            errorInfo.put("lastOccurred", lastErrorTimes.get(errorType));
            stats.put(errorType, errorInfo);
        });
        
        return stats;
    }

    /**
     * 에러 카운터 리셋 (일일 배치 등에서 사용)
     */
    public static void resetErrorCounters() {
        errorCounts.clear();
        lastErrorTimes.clear();
        log.info("📊 에러 카운터 리셋 완료");
    }

    // ===========================================
    // 알림 및 정리 작업
    // ===========================================

    /**
     * Redis 연결 실패 알림
     */
    private static void notifyRedisConnectionFailure(String operation, String key, long failureCount) {
        // 실제 환경에서는 Slack, Discord, 이메일 등으로 알림
        log.error("🚨 [긴급] Redis 연결 실패 알림 - 작업: {}, 키: {}, 실패횟수: {}", 
                operation, key, failureCount);
    }

    /**
     * 만료된 채팅방 정리
     */
    private static void cleanupExpiredChatRoom(String key) {
        log.info("🧽 만료된 채팅방 정리 - 키: {}", key);
        // 추가 정리 작업 (관련 세션, 메타데이터 등)
    }

    /**
     * 만료된 사용자 세션 정리
     */
    private static void cleanupExpiredUserSession(String key) {
        log.info("🧽 만료된 사용자 세션 정리 - 키: {}", key);
        // WebSocket 연결 해제, 세션 매핑 정리 등
    }


    // ===========================================
    // 헬스체크 및 진단
    // ===========================================

    /**
     * 채팅 시스템 헬스체크
     */
    public static Map<String, Object> getHealthCheck() {
        Map<String, Object> health = new ConcurrentHashMap<>();
        
        // Redis 연결 상태
        long redisErrors = getErrorCount("REDIS_CONNECTION_FAILED");
        health.put("redis_status", redisErrors < 5 ? "HEALTHY" : "DEGRADED");
        
        // WebSocket 연결 상태
        long websocketErrors = getErrorCount("WEBSOCKET_CONNECTION_ERROR");
        health.put("websocket_status", websocketErrors < 10 ? "HEALTHY" : "DEGRADED");
        
        // 전체 시스템 상태
        boolean isHealthy = redisErrors < 5 && websocketErrors < 10;
        health.put("overall_status", isHealthy ? "HEALTHY" : "DEGRADED");
        health.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        
        return health;
    }
}