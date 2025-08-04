package com.ssafy.schedule.util;

import com.ssafy.schedule.common.GameResult;

/**
 * 경기 결과 계산을 위한 유틸리티 클래스
 */
public class GameResultCalculator {

    /**
     * 홈팀과 원정팀의 점수를 비교하여 경기 결과를 계산
     * 
     * @param homeScore 홈팀 점수
     * @param awayScore 원정팀 점수
     * @return 경기 결과 (HOME_WIN, AWAY_WIN, DRAW)
     */
    public static GameResult calculateResult(Integer homeScore, Integer awayScore) {
        if (homeScore > awayScore) {
            return GameResult.HOME_WIN;
        } else if (awayScore > homeScore) {
            return GameResult.AWAY_WIN;
        } else {
            return GameResult.DRAW;
        }
    }
}