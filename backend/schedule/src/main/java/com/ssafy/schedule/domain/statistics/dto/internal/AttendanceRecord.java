package com.ssafy.schedule.domain.statistics.dto.internal;

import com.ssafy.schedule.global.constant.GameResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * 내부 처리용 직관 기록 DTO
 * - Redis에서 조회한 직관 기록과 DB에서 조회한 경기 결과를 결합
 * - 통계 계산을 위한 완전한 데이터 구조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    
    // 기본 정보
    private Long userId;
    private Long gameId;
    private LocalDateTime gameDateTime;

    // 시즌 정보 (통산/시즌별 구분용)
    private String season; // "all" 또는 "2025" 등
    
    // 경기 정보 (DB에서 조회)
    private String homeTeam;
    private String awayTeam;
    private String userTeam; // 사용자 응원팀
    private GameResult gameResult; // HOME_WIN, AWAY_WIN, DRAW

    // 구장 정보 (구장별 승률 계산용)
    private String stadium; // 구장명
    
    // 사용자 관점 결과
    private UserGameResult userGameResult; // WIN, LOSS, DRAW
    
    public enum UserGameResult {
        WIN, LOSS, DRAW
    }
    
    // 사용자 관점에서의 경기 결과 계산
    public UserGameResult calculateUserGameResult() {
        if (gameResult == GameResult.DRAW) {
            return UserGameResult.DRAW;
        }
        
        boolean isUserTeamHome = userTeam.equals(homeTeam);
        boolean isUserTeamWin = (isUserTeamHome && gameResult == GameResult.HOME_WIN) ||
                               (!isUserTeamHome && gameResult == GameResult.AWAY_WIN);
        
        return isUserTeamWin ? UserGameResult.WIN : UserGameResult.LOSS;
    }
    
    // 홈/원정 여부 확인 (사용자 응원팀 기준)
    public boolean isHomeGame() {
        return userTeam.equals(homeTeam);
    }
    
    // 요일 정보
    public DayOfWeek getDayOfWeek() {
        return gameDateTime.getDayOfWeek();
    }
    
    // 상대팀 정보
    public String getOpponentTeam() {
        return userTeam.equals(homeTeam) ? awayTeam : homeTeam;
    }
}