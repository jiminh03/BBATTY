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
    // USER 도메인 - 사용자 관련
    // ===========================================

    /** 당일 직관 인증 여부: user:attendance:daily:{date}:{user_id} */
    public static final String DAILY_ATTENDANCE = "user:attendance:daily:";

    /** 사용자 승률 캐시: user:stats:winrate:{user_id} */
    public static final String STATS_WINRATE = "user:stats:winrate:";

    /** 사용자 현재 직관 연승: user:stats:current_streak:{user_id} */
    public static final String STATS_CURRENT_WIN_STREAK = "user:stats:current_streak:";

    /** 사용자 최장 직관 연승: user:stats:max_streak:{user_id} */
    public static final String STATS_MAX_WIN_STREAK = "user:stats:max_streak:";

    /** 전체 순위에서 내 순위: user:rank:global:{user_id} */
    public static final String RANK_GLOBAL = "user:rank:global:";

    /** 팀내 순위에서 내 순위: user:rank:team:{team_id}:{user_id} */
    public static final String RANK_TEAM = "user:rank:team:";

    /** 전체 백분위: user:percentile:global:{user_id} */
    public static final String PERCENTILE_GLOBAL = "user:percentile:global:";

    /** 팀내 백분위: user:percentile:team:{team_id}:{user_id} */
    public static final String PERCENTILE_TEAM = "user:percentile:team:";


    // ===========================================
    // RANKING 도메인 - 랭킹 전용
    // ===========================================

    /** 전체 상위 10명: ranking:global:top10 (Sorted Set) */
    public static final String GLOBAL_TOP10 = "ranking:global:top10";

    /** 팀별 상위 10명: ranking:team:{team_id}:top10 (Sorted Set) */
    public static final String TEAM_TOP10 = "ranking:team:top10";

    // ===========================================
    // GAME 도메인 - 경기 관련
    // ===========================================

    /** 경기 정보 캐시: game:info:{game_id} */
    public static final String INFO_CACHE = "game:info:";


    // ===========================================
    // STATISTICS 도메인 - 통계 업데이트용
    // ===========================================

    /** 당일 직관 인증한 전체 사용자 목록: stats:daily_attendees:{date} (Set) */
    public static final String DAILY_ATTENDEES = "stats:daily_attendees:";
}