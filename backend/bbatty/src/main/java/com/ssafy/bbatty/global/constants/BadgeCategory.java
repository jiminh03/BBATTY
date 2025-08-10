package com.ssafy.bbatty.global.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 뱃지 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum BadgeCategory {
    STADIUM_CONQUEST("구장정복", "각 구장 첫 방문시 획득"),
    SEASON_WINS("승리", "시즌별 승수 달성시 획득"), 
    SEASON_GAMES("직관경기", "시즌별 직관 경기수 달성시 획득");
    
    private final String displayName;
    private final String description;
}