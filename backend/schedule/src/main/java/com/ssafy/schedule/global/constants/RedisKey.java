package com.ssafy.schedule.global.constants;

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
    
    /** 당일 직관 인증한 전체 사용자 목록: attendance:daily:attendees:{date} (Hash - userId: teamId:gameId) */
    public static final String ATTENDANCE_DAILY_ATTENDEES = "attendance:daily:attendees:";
    
    /** 사용자별 직관 기록: user:attendance:records:{user_id}:{season} (Sorted Set) */
    public static final String USER_ATTENDANCE_RECORDS = "user:attendance:records:";
    
    // ===========================================
    // STATS 도메인 - 모든 통계 데이터 통합
    // ===========================================
    
    /** 사용자 기본 승률 캐시: stats:user:winrate:{user_id}:{season} */
    public static final String STATS_USER_WINRATE = "stats:user:winrate:";
    
    /** 사용자 연승 통계 (현재/최장/시즌별): stats:user:streak:{user_id}:{season} */
    public static final String STATS_USER_STREAK = "stats:user:streak:";
    
    /** 사용자 상세 통계: stats:user:detailed:{user_id}:{season} (Hash) */
    public static final String STATS_USER_DETAILED = "stats:user:detailed:";
    
    // ===========================================
    // RANKING 도메인 - 이번 시즌 승률 기준 랭킹 (Top 10)
    // ===========================================
    
    /** 전체 상위 10명: ranking:global:top10 (Sorted Set) */
    public static final String RANKING_GLOBAL_TOP10 = "ranking:global:top10";
    
    /** 팀별 상위 10명: ranking:team:{team_id}:top10 (Sorted Set) */
    public static final String RANKING_TEAM_TOP10 = "ranking:team:";
    
    /** 전체 순위에서 내 순위: ranking:user:global:{user_id} */
    public static final String RANKING_USER_GLOBAL = "ranking:user:global:";
    
    /** 팀내 순위에서 내 순위: ranking:user:team:{team_id}:{user_id} */
    public static final String RANKING_USER_TEAM = "ranking:user:team:";
    
    /** 전체 백분위: ranking:percentile:global:{user_id} */
    public static final String RANKING_PERCENTILE_GLOBAL = "ranking:percentile:global:";
    
    /** 팀내 백분위: ranking:percentile:team:{team_id}:{user_id} */
    public static final String RANKING_PERCENTILE_TEAM = "ranking:percentile:team:";
    
    // ===========================================
    // BADGE 도메인 - 뱃지 획득 여부 캐싱
    // ===========================================
    
    /** 사용자 구장 뱃지 획득 여부: badge:stadium:{user_id}:{stadium_name} */
    public static final String BADGE_STADIUM = "badge:stadium:";
    
    /** 사용자 시즌 승리 뱃지 획득 여부: badge:wins:{user_id}:{season}:{win_count} */
    public static final String BADGE_WINS = "badge:wins:";
    
    /** 사용자 시즌 직관 경기 뱃지 획득 여부: badge:games:{user_id}:{season}:{game_count} */
    public static final String BADGE_GAMES = "badge:games:";

    public static final String NEWS_SUMMARY = "news:summary:";

}