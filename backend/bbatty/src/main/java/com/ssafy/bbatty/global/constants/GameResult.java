package com.ssafy.bbatty.global.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameResult {
    HOME_WIN("홈팀 승리"),
    AWAY_WIN("어웨이팀 승리"),
    DRAW("무승부"),
    CANCELLED("경기 취소");

    private final String description;
}
