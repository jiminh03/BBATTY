package com.ssafy.schedule.global.constant;

/**
 * Redis 키 상수
 * - schedule 서버에서 사용하는 통계 키들
 * - bbatty 서버의 직관 기록 키들도 참조
 */
public class RedisKey {
    
    // ===========================================
    // bbatty 서버의 직관 기록 키들 (조회용)
    // ===========================================
    
    /** 사용자별 경기 직관 인증 여부: user:attendance:{user_id}:{game_id} */
    public static final String USER_ATTENDANCE_GAME = "user:attendance:";
    
    /** 당일 직관 인증한 전체 사용자 목록: attendance:daily:attendees:{date} (Set) */
    public static final String ATTENDANCE_DAILY_ATTENDEES = "attendance:daily:attendees:";
    
    /** 사용자별 직관 기록: user:attendance:records:{user_id}:{season} (Sorted Set) */
    public static final String USER_ATTENDANCE_RECORDS = "user:attendance:records:";
    
    // ===========================================
    // STATS 도메인 - 모든 통계 데이터 통합
    // ===========================================
    
    /** 사용자 기본 승률 캐시: stats:user:winrate:{user_id} */
    public static final String STATS_USER_WINRATE = "stats:user:winrate:";
    
    /** 사용자 연승 통계 (현재/최장/시즌별): stats:user:streak:{user_id} */
    public static final String STATS_USER_STREAK = "stats:user:streak:";
    
    /** 사용자 상세 통계: stats:user:detailed:{user_id}:{season} (Hash) */
    public static final String STATS_USER_DETAILED = "stats:user:detailed:";
}