package com.ssafy.schedule.global.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateFilterUtil {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 오늘부터 7일 전까지의 데이터인지 확인합니다. (서울 시간 기준)
     * @param pubDate RFC 822 형식의 날짜 문자열 (예: "Mon, 08 Jan 2024 15:30:00 +0900")
     * @return 7일 이내 데이터면 true, 아니면 false
     */
    public static boolean isWithinSevenDays(String pubDate) {
        try {
            // 네이버 뉴스 API의 pubDate 형식: "Mon, 08 Jan 2024 15:30:00 +0900"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            ZonedDateTime newsDateTime = ZonedDateTime.parse(pubDate, formatter);
            ZonedDateTime seoulNewsTime = newsDateTime.withZoneSameInstant(SEOUL_ZONE);
            
            ZonedDateTime sevenDaysAgo = ZonedDateTime.now(SEOUL_ZONE).minusDays(7);
            
            return seoulNewsTime.isAfter(sevenDaysAgo);

        } catch (Exception e) {
            return false;
        }
    }
}