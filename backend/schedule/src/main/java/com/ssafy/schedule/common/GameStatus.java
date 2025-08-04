package com.ssafy.schedule.common;

/**
 * 경기 상태를 나타내는 열거형
 * - SCHEDULED: 예정된 경기 (아직 시작되지 않음)
 * - FINISHED: 완료된 경기 (결과가 확정됨)
 * - CANCELLED: 취소된 경기 (우천 취소 등)
 */
public enum GameStatus {
    SCHEDULED,  // 경기 예정
    FINISHED,   // 경기 종료
    CANCELLED   // 경기 취소
}
