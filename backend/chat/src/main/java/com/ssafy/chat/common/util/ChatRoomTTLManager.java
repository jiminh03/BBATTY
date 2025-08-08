package com.ssafy.chat.common.util;

import com.ssafy.chat.common.util.KSTTimeUtil;

import com.ssafy.chat.common.enums.ChatRoomType;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 채팅방 TTL(Time To Live) 관리 중앙화 클래스
 * - WATCH/MATCH: 3시간 세션 (통일)
 * - 채팅방: 자정까지 또는 경기 날짜 자정까지
 * - TRAFFIC: 트래픽 모니터링 3분 + 1분 버퍼
 * - GAME_INFO: 게임 정보 14일
 * - AUTH_RESULT: 인증 결과 10분
 */
@Slf4j
public class ChatRoomTTLManager {

    /** 채팅 세션 TTL: 3시간 (관전/매칭 통일) */
    public static final Duration CHAT_SESSION_TTL = Duration.ofHours(3);

    /** 트래픽 모니터링 TTL: 3분 + 1분 버퍼 */
    public static final Duration TRAFFIC_MONITORING_TTL = Duration.ofMinutes(4);


    /** 채팅 인증 결과 TTL: 10분 */
    public static final Duration CHAT_AUTH_RESULT_TTL = Duration.ofMinutes(10);

    /** 최소 TTL 보장: 1분 (자정 직전 접근 대비) */
    public static final Duration MINIMUM_TTL = Duration.ofMinutes(1);

    /** 기본 Fallback TTL: 1시간 */
    public static final Duration DEFAULT_FALLBACK_TTL = Duration.ofHours(1);

    /**
     * 채팅방 타입별 세션 TTL 반환 (관전/매칭 통일)
     */
    public static Duration getSessionTTL(ChatRoomType chatRoomType) {
        return switch (chatRoomType) {
            case WATCH, MATCH -> CHAT_SESSION_TTL;
            default -> {
                log.warn("알 수 없는 채팅방 타입: {}, 기본 TTL 적용", chatRoomType);
                yield DEFAULT_FALLBACK_TTL;
            }
        };
    }

    /**
     * 채팅 세션 TTL 반환 (3시간 고정)
     */
    public static Duration getChatSessionTTL() {
        return CHAT_SESSION_TTL;
    }

    /**
     * 당일 자정까지의 TTL 계산
     * 관전 채팅방에서 사용 - 당일 자정에 만료
     */
    public static Duration getTTLUntilMidnight() {
        LocalDateTime now = KSTTimeUtil.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration duration = Duration.between(now, midnight);
        
        // 최소 TTL 보장
        if (duration.isZero() || duration.isNegative() || duration.compareTo(MINIMUM_TTL) < 0) {
            log.warn("자정까지 TTL이 너무 짧음: {}분, 최소 TTL 적용: {}분", 
                    duration.toMinutes(), MINIMUM_TTL.toMinutes());
            return MINIMUM_TTL;
        }
        
        log.debug("자정까지 TTL 계산: {}시간 {}분", duration.toHours(), duration.toMinutesPart());
        return duration;
    }

    /**
     * 경기 날짜 기준 TTL 계산
     * 매칭 채팅방에서 사용 - 경기 날짜 자정까지 만료
     */
    public static Duration getTTLUntilGameMidnight(LocalDateTime gameDateTime) {
        if (gameDateTime == null) {
            log.warn("게임 시간이 null, 기본 TTL 적용");
            return DEFAULT_FALLBACK_TTL;
        }

        LocalDate gameDate = gameDateTime.toLocalDate();
        LocalDateTime midnightAfterGame = gameDate.plusDays(1).atTime(LocalTime.MIDNIGHT);
        Duration ttl = Duration.between(KSTTimeUtil.now(), midnightAfterGame);
        
        // TTL이 음수이거나 너무 짧으면 기본 TTL 적용
        if (ttl.isNegative() || ttl.toMinutes() < MINIMUM_TTL.toMinutes()) {
            log.warn("경기 기준 TTL이 유효하지 않음 - 게임시간: {}, 계산된TTL: {}분, 기본TTL 적용", 
                    gameDateTime, ttl.toMinutes());
            return DEFAULT_FALLBACK_TTL;
        }
        
        log.debug("경기 기준 TTL 계산 - 게임날짜: {}, TTL: {}시간", gameDate, ttl.toHours());
        return ttl;
    }

    /**
     * 트래픽 모니터링 TTL 반환
     */
    public static Duration getTrafficMonitoringTTL() {
        return TRAFFIC_MONITORING_TTL;
    }


    /**
     * 채팅 인증 결과 TTL 반환
     */
    public static Duration getChatAuthResultTTL() {
        return CHAT_AUTH_RESULT_TTL;
    }

    // ===========================================
    // TTL 검증 및 유틸리티
    // ===========================================

    /**
     * TTL이 유효한지 검증
     */
    public static boolean isValidTTL(Duration ttl) {
        return ttl != null && !ttl.isNegative() && !ttl.isZero();
    }

    /**
     * TTL을 안전한 값으로 보정
     */
    public static Duration ensureSafeTTL(Duration ttl) {
        if (!isValidTTL(ttl)) {
            log.warn("유효하지 않은 TTL: {}, 기본 TTL 적용", ttl);
            return DEFAULT_FALLBACK_TTL;
        }
        
        if (ttl.compareTo(MINIMUM_TTL) < 0) {
            log.warn("TTL이 최소값보다 작음: {}분, 최소 TTL 적용", ttl.toMinutes());
            return MINIMUM_TTL;
        }
        
        return ttl;
    }

    /**
     * TTL을 초 단위로 변환 (Redis expire 명령용)
     */
    public static long toSeconds(Duration ttl) {
        return ensureSafeTTL(ttl).getSeconds();
    }

    /**
     * TTL을 분 단위로 변환 (로깅용)
     */
    public static long toMinutes(Duration ttl) {
        return ensureSafeTTL(ttl).toMinutes();
    }

    /**
     * TTL을 시간 단위로 변환 (로깅용)
     */
    public static long toHours(Duration ttl) {
        return ensureSafeTTL(ttl).toHours();
    }
}