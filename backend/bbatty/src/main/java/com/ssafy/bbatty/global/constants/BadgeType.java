package com.ssafy.bbatty.global.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 뱃지 타입 및 획득 조건 정의
 */
@Getter
@RequiredArgsConstructor
public enum BadgeType {
    
    // ========================================
    // 구장별 방문 뱃지 (영구, 통산) - 9개
    // ========================================
    JAMSIL_STADIUM("잠실야구장 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.JAMSIL),
    GOCHEOK_STADIUM("고척스카이돔 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.GOCHEOK),
    SUWON_STADIUM("수원KT위즈파크 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.SUWON),
    INCHEON_STADIUM("인천SSG랜더스필드 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.INCHEON),
    DAEJEON_STADIUM("대전한화생명볼파크 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.DAEJEON),
    GWANGJU_STADIUM("광주기아챔피언스필드 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.GWANGJU),
    DAEGU_STADIUM("대구삼성라이온즈파크 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.DAEGU),
    BUSAN_STADIUM("부산사직야구장 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.BUSAN),
    CHANGWON_STADIUM("창원NC파크 첫 방문", BadgeCategory.STADIUM_CONQUEST, Stadium.CHANGWON),
    
    // ========================================
    // 승리 뱃지 (시즌별) - 3개
    // ========================================
    WIN_1("시즌 첫 승리 달성", BadgeCategory.SEASON_WINS, 1),
    WIN_5("시즌 5승 달성", BadgeCategory.SEASON_WINS, 5),
    WIN_15("시즌 15승 달성", BadgeCategory.SEASON_WINS, 15),
    
    // ========================================
    // 직관 경기 수 뱃지 (시즌별) - 3개
    // ========================================
    GAME_1("시즌 첫 직관", BadgeCategory.SEASON_GAMES, 1),
    GAME_10("시즌 10경기 직관", BadgeCategory.SEASON_GAMES, 10),
    GAME_30("시즌 30경기 직관", BadgeCategory.SEASON_GAMES, 30);
    
    private final String description;
    private final BadgeCategory category;
    private final Object requirement; // Stadium 또는 Integer
    
    /**
     * 구장 뱃지인지 확인
     */
    public boolean isStadiumBadge() {
        return category == BadgeCategory.STADIUM_CONQUEST;
    }
    
    /**
     * 시즌 기반 뱃지인지 확인
     */
    public boolean isSeasonBased() {
        return !isStadiumBadge();
    }
}