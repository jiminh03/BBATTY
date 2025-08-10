package com.ssafy.schedule.global.util;

import com.ssafy.schedule.domain.statistics.dto.internal.AttendanceRecord;
import com.ssafy.schedule.global.constants.GameResult;
import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.global.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 직관 기록 JSON 파싱 유틸리티
 * - JSON 파싱 로직을 통합하여 중복 제거
 * - 안전한 파싱과 예외 처리 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceRecordParser {
    
    private final GameRepository gameRepository;
    
    /**
     * JSON 문자열을 AttendanceRecord로 변환
     * @param recordJson JSON 문자열
     * @param userTeamId 사용자 팀 ID
     * @return AttendanceRecord 또는 null (파싱 실패시)
     */
    public AttendanceRecord parseAttendanceRecord(String recordJson, Long userTeamId) {
        try {
            // 필수 필드 추출
            Long gameId = extractJsonLong(recordJson, "gameId");
            String homeTeam = extractJsonString(recordJson, "homeTeam");
            String awayTeam = extractJsonString(recordJson, "awayTeam");
            String dateTimeStr = extractJsonString(recordJson, "dateTime");
            String stadium = extractJsonString(recordJson, "stadium");
            
            if (gameId == null || homeTeam == null || awayTeam == null || 
                dateTimeStr == null || stadium == null) {
                log.warn("JSON에서 필수 필드 누락: {}", recordJson);
                return null;
            }
            
            // 경기 시간 파싱
            LocalDateTime gameDateTime = LocalDateTime.parse(dateTimeStr);
            String season = String.valueOf(gameDateTime.getYear());
            
            // DB에서 최신 경기 정보 조회
            Optional<Game> gameOpt = gameRepository.findById(gameId);
            if (gameOpt.isEmpty()) {
                log.warn("DB에서 게임을 찾을 수 없음: gameId={}", gameId);
                return null;
            }
            
            Game game = gameOpt.get();
            
            return AttendanceRecord.builder()
                    .userId(userTeamId) // 호환성을 위해 userTeamId를 userId에 저장
                    .gameId(gameId)
                    .gameDateTime(gameDateTime)
                    .season(season)
                    .homeTeamId(game.getHomeTeam().getId())
                    .awayTeamId(game.getAwayTeam().getId())
                    .userTeamId(userTeamId)
                    .gameResult(game.getResult())
                    .stadium(stadium)
                    .userGameResult(calculateUserGameResult(game.getResult(), userTeamId, game.getHomeTeam().getId()))
                    .build();
                    
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", recordJson, e);
            return null;
        }
    }
    
    /**
     * 간단한 AttendanceRecord 생성 (뱃지 업데이트용)
     */
    public AttendanceRecord parseSimpleAttendanceRecord(String recordJson, Game game) {
        try {
            Long gameId = extractJsonLong(recordJson, "gameId");
            String dateTimeStr = extractJsonString(recordJson, "dateTime");
            
            if (gameId == null || dateTimeStr == null) {
                log.warn("JSON에서 필수 필드 누락: {}", recordJson);
                return null;
            }
            
            LocalDateTime gameDateTime = LocalDateTime.parse(dateTimeStr);
            String season = String.valueOf(gameDateTime.getYear());
            
            return AttendanceRecord.builder()
                    .gameId(gameId)
                    .gameDateTime(gameDateTime)
                    .season(season)
                    .homeTeamId(game.getHomeTeam().getId())
                    .awayTeamId(game.getAwayTeam().getId())
                    .gameResult(game.getResult())
                    .stadium(game.getStadium())
                    .build();
                    
        } catch (Exception e) {
            log.error("간단한 JSON 파싱 실패: {}", recordJson, e);
            return null;
        }
    }
    
    /**
     * 사용자 관점에서 경기 결과 계산
     */
    private AttendanceRecord.UserGameResult calculateUserGameResult(GameResult gameResult, Long userTeamId, Long homeTeamId) {
        if (gameResult == GameResult.DRAW) {
            return AttendanceRecord.UserGameResult.DRAW;
        }
        
        boolean isUserTeamHome = userTeamId.equals(homeTeamId);
        boolean isUserTeamWin = (isUserTeamHome && gameResult == GameResult.HOME_WIN) ||
                (!isUserTeamHome && gameResult == GameResult.AWAY_WIN);
                
        return isUserTeamWin ? AttendanceRecord.UserGameResult.WIN : AttendanceRecord.UserGameResult.LOSS;
    }
    
    /**
     * JSON에서 Long 값 추출
     */
    private Long extractJsonLong(String json, String key) {
        try {
            String value = extractJsonValue(json, key);
            return value != null ? Long.valueOf(value) : null;
        } catch (NumberFormatException e) {
            log.debug("숫자 파싱 실패: key={}, value={}", key, extractJsonValue(json, key));
            return null;
        }
    }
    
    /**
     * JSON에서 String 값 추출
     */
    private String extractJsonString(String json, String key) {
        return extractJsonValue(json, key);
    }
    
    /**
     * JSON에서 특정 키의 값 추출 (안전한 파서)
     */
    private String extractJsonValue(String json, String key) {
        try {
            if (json == null || key == null) {
                return null;
            }
            
            String pattern = "\"" + key + "\":";
            int startIndex = json.indexOf(pattern);
            if (startIndex == -1) {
                return null;
            }
            
            startIndex += pattern.length();
            
            // 공백 제거
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }
            
            if (startIndex >= json.length()) {
                return null;
            }
            
            // 값의 끝 위치 찾기
            int endIndex;
            if (json.charAt(startIndex) == '"') {
                // 문자열 값
                startIndex++; // 시작 따옴표 건너뛰기
                endIndex = json.indexOf('"', startIndex);
            } else {
                // 숫자 값
                endIndex = json.indexOf(',', startIndex);
                if (endIndex == -1) {
                    endIndex = json.indexOf('}', startIndex);
                }
            }
            
            if (endIndex == -1 || endIndex <= startIndex) {
                return null;
            }
            
            return json.substring(startIndex, endIndex).trim();
            
        } catch (Exception e) {
            log.debug("JSON 값 추출 실패: key={}, json={}", key, json, e);
            return null;
        }
    }
}