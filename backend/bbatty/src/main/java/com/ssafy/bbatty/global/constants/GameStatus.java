package com.ssafy.bbatty.global.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameStatus {
    SCHEDULED("경기 예정"),
    LIVE("경기 진행중"),
    FINISHED("경기 종료"),
    CANCELLED("경기 취소");

    private final String description;
}
