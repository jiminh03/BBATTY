package com.ssafy.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 채팅 시스템 중앙화된 설정
 * application.yml의 chat.* 설정값들을 관리
 */
@Configuration
@ConfigurationProperties(prefix = "chat")
@Data
public class ChatConfiguration {
    
    // ===========================================
    // 세션 관련 설정
    // ===========================================
    
    /**
     * 채팅 세션 TTL (기본: 3시간)
     */
    private Duration sessionTtl = Duration.ofHours(3);
    
    /**
     * 인증 타임아웃 (기본: 10분)
     */
    private Duration authTimeout = Duration.ofMinutes(10);
    
    /**
     * 인증 결과 TTL (기본: 10분)
     */
    private Duration authResultTtl = Duration.ofMinutes(10);
    
    /**
     * 세션 토큰 TTL (기본: 30분)
     */
    private Duration sessionTokenTtl = Duration.ofMinutes(30);
    
    // ===========================================
    // 메시지 및 이력 관련 설정
    // ===========================================
    
    /**
     * 기본 메시지 이력 조회 개수
     */
    private int defaultMessageLimit = 50;
    
    /**
     * 최대 메시지 이력 조회 개수
     */
    private int maxMessageLimit = 200;
    
    /**
     * 메시지 이력 캐시 TTL (기본: 5분)
     */
    private Duration messageHistoryCacheTtl = Duration.ofMinutes(5);
    
    // ===========================================
    // 트래픽 모니터링 설정
    // ===========================================
    
    /**
     * 트래픽 모니터링 TTL (기본: 4분)
     */
    private Duration trafficMonitoringTtl = Duration.ofMinutes(4);
    
    /**
     * 트래픽 급증 감지 임계값 (분당 메시지 수)
     */
    private int trafficSpikeThreshold = 100;
    
    /**
     * 트래픽 모니터링 윈도우 크기 (분)
     */
    private int trafficMonitoringWindowMinutes = 3;
    
    // ===========================================
    // WebSocket 세션 관리 설정
    // ===========================================
    
    /**
     * 세션 정리 주기 (기본: 30초)
     */
    private Duration sessionCleanupInterval = Duration.ofSeconds(30);
    
    /**
     * 비활성 세션 정리 임계값 (기본: 5분)
     */
    private Duration inactiveSessionThreshold = Duration.ofMinutes(5);
    
    /**
     * 최대 동시 연결 세션 수 (서버 인스턴스당)
     */
    private int maxConcurrentSessions = 10000;
    
    // ===========================================
    // Redis 관련 설정
    // ===========================================
    
    /**
     * Redis 키 스캔 배치 크기
     */
    private long redisKeyScanCount = 1000L;
    
    /**
     * Redis Pub/Sub 메시지 버퍼 크기
     */
    private int redisPubSubBufferSize = 1000;
    
    // ===========================================
    // 채팅방 관리 설정
    // ===========================================
    
    /**
     * 빈 채팅방 정리 지연 시간 (기본: 30초)
     */
    private Duration emptyChatRoomCleanupDelay = Duration.ofSeconds(30);
    
    /**
     * 매칭 채팅방 목록 페이지 크기 (기본: 20개)
     */
    private int matchChatRoomPageSize = 20;
    
    /**
     * 최대 매칭 채팅방 목록 페이지 크기 (기본: 100개)
     */
    private int maxMatchChatRoomPageSize = 100;
    
    // ===========================================
    // WebSocket 설정
    // ===========================================
    
    /**
     * WebSocket 기본 URL
     */
    private String websocketBaseUrl = "ws://i13a403.p.ssafy.io:8084";
    
    // ===========================================
    // 분산 환경 설정
    // ===========================================
    
    /**
     * 서버 인스턴스 ID (분산 환경에서 구별용)
     */
    private String instanceId;
    
    /**
     * 분산 세션 동기화 활성화 여부
     */
    private boolean distributedSessionEnabled = true;
    
    /**
     * 🧪 테스트 모드 활성화 여부 (기본값: true - 개발 환경용)
     */
    private boolean testModeEnabled = true;
    
    /**
     * 분산 세션 하트비트 간격 (기본: 10초)
     */
    private Duration distributedSessionHeartbeatInterval = Duration.ofSeconds(10);
    
    /**
     * 분산 세션 TTL (기본: 30초)
     */
    private Duration distributedSessionTtl = Duration.ofSeconds(30);
    
    // ===========================================
    // 유효성 검증 메서드
    // ===========================================
    
    /**
     * 설정값 유효성 검증
     */
    public boolean isValidConfiguration() {
        return sessionTtl != null && !sessionTtl.isNegative()
               && authTimeout != null && !authTimeout.isNegative()
               && defaultMessageLimit > 0 && defaultMessageLimit <= maxMessageLimit
               && trafficSpikeThreshold > 0
               && maxConcurrentSessions > 0;
    }
    
    /**
     * 인스턴스 ID 자동 생성 (설정되지 않은 경우)
     */
    public String getOrGenerateInstanceId() {
        if (instanceId == null || instanceId.trim().isEmpty()) {
            instanceId = "chat-" + System.currentTimeMillis() + "-" + 
                        (int) (Math.random() * 1000);
        }
        return instanceId;
    }
}