package com.ssafy.chat.common.util;

import com.ssafy.chat.config.ChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 채팅방 관련 공통 유틸리티 클래스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomUtils {
    
    private final ChatProperties chatProperties;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * WebSocket URL 생성
     */
    public String buildWebSocketUrl(String endpoint, Object... params) {
        String baseUrl = chatProperties.getWebsocket().getBaseUrl();
        return String.format(baseUrl + endpoint, params);
    }

    /**
     * 매칭 채팅방 WebSocket URL 생성
     */
    public String buildMatchChatWebSocketUrl(String matchId) {
        return buildWebSocketUrl("/ws/match-chat/websocket?matchId=%s", matchId);
    }

    /**
     * 관전 채팅방 WebSocket URL 생성
     */
    public String buildWatchChatWebSocketUrl(String token, Long gameId, Long teamId) {
        return buildWebSocketUrl("/ws/watch-chat/websocket?token=%s&gameId=%s&teamId=%s", 
                token, gameId, teamId);
    }

    /**
     * 게임 날짜 문자열을 기준으로 TTL 계산 (게임 날짜 자정까지)
     */
    public Duration calculateTTLFromGameDate(String gameDateStr) {
        try {
            LocalDate gameDate = LocalDate.parse(gameDateStr, DATE_FORMATTER);
            LocalDateTime midnightAfterGame = gameDate.plusDays(1).atTime(LocalTime.MIDNIGHT);
            
            Duration ttl = Duration.between(KSTTimeUtil.now(), midnightAfterGame);
            
            // TTL이 음수이거나 0에 가깝다면 기본 1시간 설정
            if (ttl.isNegative() || ttl.toMinutes() < 10) {
                ttl = Duration.ofHours(1);
            }
            
            log.debug("게임 날짜: {}, TTL: {}시간", gameDate, ttl.toHours());
            return ttl;
        } catch (Exception e) {
            log.warn("게임 날짜 파싱 실패 - gameDateStr: {}", gameDateStr, e);
            return Duration.ofHours(1); // 기본값
        }
    }

    /**
     * 타임스탬프 문자열을 점수(long)로 변환
     */
    public long parseTimestampToScore(String timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp);
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.warn("타임스탬프 파싱 실패 - timestamp: {}", timestamp, e);
            return System.currentTimeMillis();
        }
    }

    /**
     * 트래픽 급증 여부 확인
     */
    public boolean isTrafficSpike(long totalMessages) {
        return totalMessages > chatProperties.getTraffic().getSpikeThreshold();
    }

    /**
     * 트래픽 모니터링 윈도우 시간(분) 반환
     */
    public int getTrafficWindowMinutes() {
        return chatProperties.getTraffic().getWindowMinutes();
    }

    /**
     * 인증 타임아웃 시간(ms) 반환
     */
    public long getAuthTimeoutMs() {
        return chatProperties.getAuth().getTimeoutMs();
    }
}