package com.ssafy.bbatty.domain.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 연결 통계 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionStats {

    /** 핸들러 타입 (GameChat, MatchChat 등) */
    private String handlerType;

    /** 총 연결 수 */
    private Long totalConnections;

    /** 연결된 사용자 목록 */
    private List<String> connectedUsers;

    /** 최근 1분간 메시지 수 */
    private Long messagesLastMinute;

    /** 최근 1시간간 메시지 수 */
    private Long messagesLastHour;

    /** 서버 시작 시간 */
    private LocalDateTime serverStartTime;

    /** 평균 메시지 처리 시간 (ms) */
    private Double averageProcessingTime;

    /** 통계 생성 시간 */
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();

    /** 방별 연결 수 */
    @Builder.Default
    private Map<String, Long> roomConnections = new HashMap<>();

    /**
     * 연결 수 증가
     */
    public void incrementConnections() {
        if (totalConnections == null) {
            totalConnections = 0L;
        }
        totalConnections++;
    }

    /**
     * 연결 수 감소
     */
    public void decrementConnections() {
        if (totalConnections != null && totalConnections > 0) {
            totalConnections--;
        }
    }

    /**
     * 특정 방의 연결 수 증가
     */
    public void incrementRoomConnections(String roomId) {
        if (roomConnections == null) {
            roomConnections = new HashMap<>();
        }
        roomConnections.merge(roomId, 1L, Long::sum);
    }

    /**
     * 특정 방의 연결 수 감소
     */
    public void decrementRoomConnections(String roomId) {
        if (roomConnections != null) {
            roomConnections.merge(roomId, -1L, Long::sum);
            // 0 이하가 되면 제거
            roomConnections.entrySet().removeIf(entry -> entry.getValue() <= 0);
        }
    }

    /**
     * 활성 채팅방 수 조회
     */
    public int getActiveRoomCount() {
        return roomConnections != null ? roomConnections.size() : 0;
    }

    /**
     * 평균 방당 사용자 수
     */
    public double getAverageUsersPerRoom() {
        if (roomConnections == null || roomConnections.isEmpty() || totalConnections == null) {
            return 0.0;
        }
        return (double) totalConnections / roomConnections.size();
    }

    /**
     * 가장 인기 있는 방 조회
     */
    public String getMostPopularRoom() {
        if (roomConnections == null || roomConnections.isEmpty()) {
            return null;
        }

        return roomConnections.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 서버 가동 시간 계산 (분)
     */
    public long getUptimeMinutes() {
        if (serverStartTime == null) {
            return 0;
        }
        return java.time.Duration.between(serverStartTime, LocalDateTime.now()).toMinutes();
    }

    /**
     * 분당 평균 메시지 처리량
     */
    public double getMessagesPerMinute() {
        long uptimeMinutes = getUptimeMinutes();
        if (uptimeMinutes == 0 || messagesLastHour == null) {
            return 0.0;
        }
        return (double) messagesLastHour / Math.min(uptimeMinutes, 60);
    }
}