package com.ssafy.chat.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;

/**
 * 한국 표준시(KST) 기준 시간 유틸리티 클래스
 */
public class KSTTimeUtil {
    
    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    
    /**
     * 현재 한국 표준시(KST) 기준 LocalDateTime 반환
     */
    public static LocalDateTime now() {
        return ZonedDateTime.now(KST_ZONE_ID).toLocalDateTime();
    }
    
    /**
     * 현재 한국 표준시(KST) 기준 ZonedDateTime 반환
     */
    public static ZonedDateTime nowZoned() {
        return ZonedDateTime.now(KST_ZONE_ID);
    }
    
    /**
     * 현재 한국 표준시(KST) 기준 시간을 문자열로 반환
     */
    public static String nowAsString() {
        return now().toString();
    }
    
    /**
     * 현재 한국 표준시(KST) 기준 시간을 timestamp(밀리초)로 반환
     */
    public static long nowAsTimestamp() {
        return nowZoned().toInstant().toEpochMilli();
    }
    
    /**
     * 현재 한국 표준시(KST) 기준 날짜를 문자열로 반환
     */
    public static String todayAsString() {
        return now().toLocalDate().toString();
    }
    
    /**
     * 현재 한국 표준시(KST) 기준 시간을 지정된 포맷으로 반환
     */
    public static String nowFormatted(DateTimeFormatter formatter) {
        return now().format(formatter);
    }
    
    /**
     * KST 시간대 ID 반환
     */
    public static ZoneId getKSTZoneId() {
        return KST_ZONE_ID;
    }
    
    /**
     * ISO 문자열을 타임스탬프(밀리초)로 변환
     */
    public static long parseToTimestamp(String isoString) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(isoString);
            return localDateTime.atZone(KST_ZONE_ID).toInstant().toEpochMilli();
        } catch (Exception e) {
            // 파싱 실패 시 현재 시간의 타임스탬프 반환
            return System.currentTimeMillis();
        }
    }
}