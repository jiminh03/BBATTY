package com.ssafy.bbatty.global.constants;

/**
 * Redis 키 전략 - 도메인 주도 설계
 *
 * 📋 키 구조: {domain}:{aggregate}:{entity_id}:{attribute}
 * 📋 TTL 전략: 도메인 특성에 맞게 설정
 */
public class RedisKey {

    // ===========================================
    // AUTH 도메인 - 인증/인가 관련
    // ===========================================

    /** 토큰 블랙리스트: auth:token:blacklist:{token_hash} */
    public static final String AUTH_TOKEN_BLACKLIST = "auth:token:blacklist:";

    // ===========================================
    // ATTENDANCE 도메인 - 직관 인증 관련
    // ===========================================

    /** 당일 직관 인증한 전체 사용자 목록: attendance:daily:attendees:{date} (Hash - userId: teamId:gameId) */
    public static final String ATTENDANCE_DAILY_ATTENDEES = "attendance:daily:attendees:";

    /** 활성 경기 목록: attendance:active:games:{date} */
    public static final String ATTENDANCE_ACTIVE_GAMES = "attendance:active:games:";

    /** 동시성 제어 락: attendance:lock:{user_id}:{game_id} */
    public static final String ATTENDANCE_LOCK = "attendance:lock:";

    // ===========================================
    // USER 도메인 - 사용자 기본 정보
    // ===========================================

    /** 사용자별 경기 직관 인증 여부: user:attendance:{user_id}:{game_id} */
    public static final String USER_ATTENDANCE_GAME = "user:attendance:";

    /** 사용자 직관 기록 목록: user:attendance:records:{user_id}:{season} (Sorted Set - score는 타임스탬프) */
    public static final String USER_ATTENDANCE_RECORDS = "user:attendance:records:";

    // ===========================================
    // STATS 도메인 - 모든 통계 데이터 통합
    // ===========================================

    /** 사용자 승률 캐시: stats:user:winrate:{user_id} */
    public static final String STATS_USER_WINRATE = "stats:user:winrate:";

    /** 사용자 연승 통계 (현재/최장/시즌별): stats:user:streak:{user_id} */
    public static final String STATS_USER_STREAK = "stats:user:streak:";

    /** 사용자 세부 통계: stats:user:detailed:{user_id}:{season} (Hash) */
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
    // BADGE 도메인 - 뱃지 획득 여부
    // ===========================================

    /** 사용자 구장 뱃지 획득 여부: badge:stadium:{user_id}:{stadium_name} */
    public static final String BADGE_STADIUM = "badge:stadium:";

    /** 사용자 시즌 승리 뱃지 획득 여부: badge:wins:{user_id}:{season}:{win_count} */
    public static final String BADGE_WINS = "badge:wins:";

    /** 사용자 시즌 직관 경기 뱃지 획득 여부: badge:games:{user_id}:{season}:{game_count} */
    public static final String BADGE_GAMES = "badge:games:";
}