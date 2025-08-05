package com.ssafy.bbatty.global.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 날짜 관련 유틸리티 클래스
 * - 진짜 중복되는 복잡한 로직만 정리
 */
public class DateUtil {
    
    /**
     * 당일 자정까지 남은 시간 계산
     * - AttendanceServiceImpl, ActiveGameRepository에서 동일한 복잡한 로직 중복
     */
    public static Duration calculateTTLUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight);
    }
    
    /**
     * 당일 자정 시간 (23:59:59)
     * - withHour(23).withMinute(59).withSecond(59) 패턴 대체
     */
    public static LocalDateTime endOfToday() {
        return LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
    }
}
