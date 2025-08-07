package com.ssafy.schedule.global.constant;

/**
 * 경기 결과를 나타내는 열거형
 * - HOME_WIN: 홈팀 승리
 * - AWAY_WIN: 원정팀 승리  
 * - DRAW: 무승부
 * - CANCELLED: 경기 취소
 */
public enum GameResult {
    HOME_WIN,    // 홈팀 승리
    AWAY_WIN,    // 원정팀 승리
    DRAW,        // 무승부
    CANCELLED    // 경기 취소
}
