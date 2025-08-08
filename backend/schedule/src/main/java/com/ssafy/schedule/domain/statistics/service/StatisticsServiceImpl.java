package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.dto.internal.AttendanceRecord;
import com.ssafy.schedule.domain.statistics.dto.response.UserBasicStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserDetailedStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserStreakStatsResponse;
import com.ssafy.schedule.domain.statistics.repository.StatisticsRedisRepository;
import com.ssafy.schedule.global.constant.GameResult;
import com.ssafy.schedule.global.constant.RedisKey;
import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.global.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 통계 서비스 구현체
 * - 승률 계산 핵심 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    
    private final StatisticsRedisRepository statisticsRedisRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GameRepository gameRepository;
    
    @Override
    public UserBasicStatsResponse calculateUserBasicStats(Long userId, String season, String userTeam) {
        log.info("사용자 기본 통계 계산 시작: userId={}, season={}, userTeam={}", userId, season, userTeam);
        
        // 캐시 확인 (시즌별 키 구분)
        Object cached = statisticsRedisRepository.getUserWinRate(userId, season);
        if (cached instanceof UserBasicStatsResponse) {
            return (UserBasicStatsResponse) cached;
        }
        
        // 선택 시즌 직관 기록 조회 및 계산
        List<AttendanceRecord> records = getAttendanceRecords(userId, season, userTeam);
        
        UserBasicStatsResponse response = buildBasicStats(userId, season, records);
        
        // 캐시 저장
        statisticsRedisRepository.saveUserWinRate(userId, season, response);
        
        log.info("사용자 기본 통계 계산 완료: userId={}, season={}, winRate={}", userId, season, response.getWinRate());
        return response;
    }
    
    @Override
    public UserDetailedStatsResponse calculateUserDetailedStats(Long userId, String season, String userTeam) {
        log.info("사용자 상세 통계 계산 시작: userId={}, season={}, userTeam={}", userId, season, userTeam);
        
        // 캐시 확인
        Object cached = statisticsRedisRepository.getUserDetailedStats(userId, season);
        if (cached instanceof UserDetailedStatsResponse) {
            return (UserDetailedStatsResponse) cached;
        }
        
        // 직관 기록 조회 및 계산
        List<AttendanceRecord> records = getAttendanceRecords(userId, season, userTeam);
        
        UserDetailedStatsResponse response = buildDetailedStats(userId, season, records);
        
        // 캐시 저장
        statisticsRedisRepository.saveUserDetailedStats(userId, season, response);
        
        log.info("사용자 상세 통계 계산 완료: userId={}, season={}", userId, season);
        return response;
    }
    
    @Override
    public UserStreakStatsResponse calculateUserStreakStats(Long userId, String userTeam) {
        log.info("사용자 연승 통계 계산 시작: userId={}, userTeam={}", userId, userTeam);
        
        // 캐시 확인
        Object cached = statisticsRedisRepository.getUserStreakStats(userId);
        if (cached instanceof UserStreakStatsResponse) {
            return (UserStreakStatsResponse) cached;
        }
        
        // 통산 직관 기록 조회 (연승 계산을 위해)
        List<AttendanceRecord> allRecords = getAttendanceRecords(userId, "total", userTeam);
        
        UserStreakStatsResponse response = buildStreakStats(userId, allRecords);
        
        // 캐시 저장
        statisticsRedisRepository.saveUserStreakStats(userId, response);
        
        log.info("사용자 연승 통계 계산 완료: userId={}, currentStreak={}", userId, response.getCurrentWinStreak());
        return response;
    }
    
    @Override
    public void recalculateUserStats(Long userId) {
        log.info("사용자 통계 재계산 시작: userId={}", userId);
        
        // 현재 시즌 가져오기
        String currentSeason = getCurrentSeason();
        
        // 현재 시즌과 통산 통계 캐시만 삭제
        statisticsRedisRepository.clearCurrentSeasonAndTotalStats(userId, currentSeason);
        
        log.info("사용자 통계 재계산 완료: userId={}, currentSeason={}", userId, currentSeason);
    }
    
    // ===========================================
    // Private Helper Methods
    // ===========================================
    
    /**
     * 사용자의 직관 기록을 Redis USER_ATTENDANCE_RECORDS 키에서 조회
     */
    private List<AttendanceRecord> getAttendanceRecords(Long userId, String season, String userTeam) {
        List<AttendanceRecord> records = new ArrayList<>();
        
        // Redis 키 구성 (시즌별 또는 통산)
        String redisKey = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":" + season;
        
        // Redis에서 직관 기록 조회 (Sorted Set - 최신순)
        Set<Object> attendanceRecords = redisTemplate.opsForZSet().reverseRange(redisKey, 0, -1);
        
        if (attendanceRecords == null || attendanceRecords.isEmpty()) {
            log.info("Redis에서 직관 기록 없음: userId={}, season={}", userId, season);
            return records;
        }
        
        log.info("Redis에서 직관 기록 조회: userId={}, season={}, count={}", userId, season, attendanceRecords.size());
        
        // JSON 레코드들을 AttendanceRecord로 변환
        for (Object recordObj : attendanceRecords) {
            try {
                String recordJson = String.valueOf(recordObj);
                AttendanceRecord record = parseAttendanceRecord(recordJson, userTeam);
                if (record != null) {
                    records.add(record);
                }
            } catch (Exception e) {
                log.warn("직관 기록 파싱 실패: userId={}, record={}", userId, recordObj, e);
            }
        }
        
        return records;
    }
    
    /**
     * 기본 통계 빌드
     */
    private UserBasicStatsResponse buildBasicStats(Long userId, String season, List<AttendanceRecord> records) {
        int wins = 0;
        int draws = 0;
        int losses = 0;
        
        for (AttendanceRecord record : records) {
            switch (record.getUserGameResult()) {
                case WIN -> wins++;
                case DRAW -> draws++;
                case LOSS -> losses++;
            }
        }
        
        return UserBasicStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(records.size())
                .wins(wins)
                .draws(draws)
                .losses(losses)
                .winRate(UserBasicStatsResponse.calculateWinRate(wins, losses))
                .build();
    }
    
    /**
     * 상세 통계 빌드
     */
    private UserDetailedStatsResponse buildDetailedStats(Long userId, String season, List<AttendanceRecord> records) {
        // 전체 통계
        int totalGames = records.size();
        int wins = 0;
        int draws = 0;
        int losses = 0;
        
        // 카테고리별 통계 카운터
        Map<String, int[]> stadiumCounters = new HashMap<>(); // [games, wins, draws, losses]
        Map<String, int[]> opponentCounters = new HashMap<>();
        Map<String, int[]> dayOfWeekCounters = new HashMap<>();
        int[] homeCounters = new int[4]; // [games, wins, draws, losses]
        int[] awayCounters = new int[4];
        
        for (AttendanceRecord record : records) {
            // 전체 통계
            switch (record.getUserGameResult()) {
                case WIN -> wins++;
                case DRAW -> draws++;
                case LOSS -> losses++;
            }
            
            // 구장별 통계
            updateCounters(stadiumCounters, record.getStadium(), record.getUserGameResult());
            
            // 상대팀별 통계  
            updateCounters(opponentCounters, record.getOpponentTeam(), record.getUserGameResult());
            
            // 요일별 통계
            updateCounters(dayOfWeekCounters, record.getDayOfWeek().toString(), record.getUserGameResult());
            
            // 홈/원정별 통계
            if (record.isHomeGame()) {
                updateCounters(homeCounters, record.getUserGameResult());
            } else {
                updateCounters(awayCounters, record.getUserGameResult());
            }
        }
        
        // CategoryStats 객체로 변환 (승률 계산 포함)
        Map<String, UserDetailedStatsResponse.CategoryStats> stadiumStats = convertToStatsMap(stadiumCounters);
        Map<String, UserDetailedStatsResponse.CategoryStats> opponentStats = convertToStatsMap(opponentCounters);
        Map<String, UserDetailedStatsResponse.CategoryStats> dayOfWeekStats = convertToStatsMap(dayOfWeekCounters);
        UserDetailedStatsResponse.CategoryStats homeStats = convertToStats(homeCounters);
        UserDetailedStatsResponse.CategoryStats awayStats = convertToStats(awayCounters);
        
        UserDetailedStatsResponse response = UserDetailedStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(totalGames)
                .wins(wins)
                .draws(draws)
                .losses(losses)
                .stadiumStats(stadiumStats)
                .opponentStats(opponentStats)
                .dayOfWeekStats(dayOfWeekStats)
                .homeStats(homeStats)
                .awayStats(awayStats)
                .build();
        
        response.updateWinRate();
        return response;
    }
    
    /**
     * 연승 통계 빌드
     */
    private UserStreakStatsResponse buildStreakStats(Long userId, List<AttendanceRecord> allRecords) {
        String currentSeason = getCurrentSeason();
        
        int currentWinStreak = 0;
        int maxWinStreakAll = 0;
        int maxWinStreakCurrentSeason = 0;
        Map<String, Integer> maxWinStreakBySeason = new HashMap<>();
        
        int tempStreak = 0;
        Map<String, Integer> tempSeasonStreak = new HashMap<>();
        
        for (AttendanceRecord record : allRecords) {
            String recordSeason = record.getSeason();
            
            if (record.getUserGameResult() == AttendanceRecord.UserGameResult.WIN) {
                tempStreak++;
                tempSeasonStreak.put(recordSeason, tempSeasonStreak.getOrDefault(recordSeason, 0) + 1);
                
                // 전체 최장 연승 갱신
                maxWinStreakAll = Math.max(maxWinStreakAll, tempStreak);
                
                // 시즌별 최장 연승 갱신
                int seasonStreak = tempSeasonStreak.get(recordSeason);
                maxWinStreakBySeason.put(recordSeason, Math.max(maxWinStreakBySeason.getOrDefault(recordSeason, 0), seasonStreak));
                
                // 현재 시즌 최장 연승 갱신
                if (recordSeason.equals(currentSeason)) {
                    maxWinStreakCurrentSeason = Math.max(maxWinStreakCurrentSeason, seasonStreak);
                }
            } else if (record.getUserGameResult() == AttendanceRecord.UserGameResult.LOSS) {
                // 패배 시 연승 리셋
                tempStreak = 0;
                tempSeasonStreak.clear();
            }
            // 무승부는 연승에 영향 없음
        }
        
        // 현재 연승은 마지막 연승 기록
        currentWinStreak = tempStreak;
        
        return UserStreakStatsResponse.builder()
                .userId(userId)
                .currentSeason(currentSeason)
                .currentWinStreak(currentWinStreak)
                .maxWinStreakAll(maxWinStreakAll)
                .maxWinStreakCurrentSeason(maxWinStreakCurrentSeason)
                .maxWinStreakBySeason(maxWinStreakBySeason)
                .build();
    }
    
    /**
     * 카운터 맵 업데이트 (카테고리별)
     */
    private void updateCounters(Map<String, int[]> counters, String category, AttendanceRecord.UserGameResult result) {
        int[] counter = counters.computeIfAbsent(category, k -> new int[4]); // [games, wins, draws, losses]
        updateCounters(counter, result);
    }
    
    /**
     * 카운터 배열 업데이트 (홈/원정별)
     */
    private void updateCounters(int[] counter, AttendanceRecord.UserGameResult result) {
        counter[0]++; // games
        switch (result) {
            case WIN -> counter[1]++; // wins
            case DRAW -> counter[2]++; // draws  
            case LOSS -> counter[3]++; // losses
        }
    }
    
    /**
     * 카운터 맵을 CategoryStats 맵으로 변환
     */
    private Map<String, UserDetailedStatsResponse.CategoryStats> convertToStatsMap(Map<String, int[]> counters) {
        Map<String, UserDetailedStatsResponse.CategoryStats> statsMap = new HashMap<>();
        for (Map.Entry<String, int[]> entry : counters.entrySet()) {
            statsMap.put(entry.getKey(), convertToStats(entry.getValue()));
        }
        return statsMap;
    }
    
    /**
     * 카운터 배열을 CategoryStats로 변환
     */
    private UserDetailedStatsResponse.CategoryStats convertToStats(int[] counter) {
        int games = counter[0];
        int wins = counter[1];
        int draws = counter[2];
        int losses = counter[3];
        
        String winRate = "0.000";
        int decisiveGames = wins + losses;
        if (decisiveGames > 0) {
            double rate = (double) wins / decisiveGames;
            winRate = String.format("%.3f", rate);
        }
        
        return UserDetailedStatsResponse.CategoryStats.builder()
                .games(games)
                .wins(wins)
                .draws(draws)
                .losses(losses)
                .winRate(winRate)
                .build();
    }
    
    /**
     * 현재 시즌 반환
     */
    private String getCurrentSeason() {
        return String.valueOf(LocalDate.now().getYear());
    }
    
    /**
     * 날짜로부터 시즌 추출
     */
    private String extractSeason(LocalDateTime dateTime) {
        return String.valueOf(dateTime.getYear());
    }
    
    /**
     * 특정 시즌인지 확인
     */
    private boolean isInSeason(LocalDateTime dateTime, String season) {
        return extractSeason(dateTime).equals(season);
    }
    
    /**
     * 사용자 관점에서 경기 결과 계산
     */
    private AttendanceRecord.UserGameResult calculateUserGameResult(GameResult gameResult, String userTeam, String homeTeam, String awayTeam) {
        if (gameResult == GameResult.DRAW) {
            return AttendanceRecord.UserGameResult.DRAW;
        }
        
        boolean isUserTeamHome = userTeam.equals(homeTeam);
        boolean isUserTeamWin = (isUserTeamHome && gameResult == GameResult.HOME_WIN) ||
                               (!isUserTeamHome && gameResult == GameResult.AWAY_WIN);
        
        return isUserTeamWin ? AttendanceRecord.UserGameResult.WIN : AttendanceRecord.UserGameResult.LOSS;
    }
    
    /**
     * JSON 문자열을 AttendanceRecord로 변환
     * JSON 예시: {"gameId":123,"homeTeam":"KIA","awayTeam":"두산","dateTime":"2024-01-01T14:00:00","stadium":"광주","status":"FINISHED"}
     */
    private AttendanceRecord parseAttendanceRecord(String recordJson, String userTeam) {
        try {
            // gameId 추출
            Long gameId = extractJsonLong(recordJson, "gameId");
            if (gameId == null) {
                log.warn("JSON에서 gameId를 찾을 수 없음: {}", recordJson);
                return null;
            }
            
            // 기본 정보 추출 (JSON에서)
            String homeTeam = extractJsonString(recordJson, "homeTeam");
            String awayTeam = extractJsonString(recordJson, "awayTeam");
            String dateTimeStr = extractJsonString(recordJson, "dateTime");
            String stadium = extractJsonString(recordJson, "stadium");
            
            if (homeTeam == null || awayTeam == null || dateTimeStr == null || stadium == null) {
                log.warn("JSON에서 필수 필드를 찾을 수 없음: {}", recordJson);
                return null;
            }
            
            // 경기 시간 파싱
            LocalDateTime gameDateTime = LocalDateTime.parse(dateTimeStr);
            
            // DB에서 최신 경기 결과 조회 (경기 결과는 실시간으로 변할 수 있음)
            Optional<Game> gameOpt = gameRepository.findById(gameId);
            if (gameOpt.isEmpty()) {
                log.warn("DB에서 게임을 찾을 수 없음: gameId={}", gameId);
                return null;
            }
            
            Game game = gameOpt.get();
            
            return AttendanceRecord.builder()
                    .userId(null) // 통계 계산에서는 불필요
                    .gameId(gameId)
                    .gameDateTime(gameDateTime)
                    .season(extractSeason(gameDateTime))
                    .homeTeam(homeTeam)
                    .awayTeam(awayTeam)
                    .userTeam(userTeam)
                    .gameResult(game.getResult()) // DB에서 최신 결과
                    .stadium(stadium)
                    .userGameResult(calculateUserGameResult(game.getResult(), userTeam, homeTeam, awayTeam))
                    .build();
                    
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", recordJson, e);
            return null;
        }
    }
    
    /**
     * JSON에서 Long 값 추출
     */
    private Long extractJsonLong(String json, String key) {
        try {
            String value = extractJsonValue(json, key);
            return value != null ? Long.valueOf(value) : null;
        } catch (NumberFormatException e) {
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
     * JSON에서 특정 키의 값 추출 (간단한 파서)
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int startIndex = json.indexOf(pattern);
            if (startIndex == -1) return null;
            
            startIndex += pattern.length();
            
            // 값의 시작 위치 찾기 (공백 제거)
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }
            
            // 값의 끝 위치 찾기
            int endIndex;
            if (json.charAt(startIndex) == '"') {
                // 문자열 값인 경우
                startIndex++; // 시작 따옴표 제거
                endIndex = json.indexOf('"', startIndex);
            } else {
                // 숫자 값인 경우
                endIndex = json.indexOf(',', startIndex);
                if (endIndex == -1) {
                    endIndex = json.indexOf('}', startIndex);
                }
            }
            
            if (endIndex == -1) return null;
            
            return json.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            log.debug("JSON 값 추출 실패: key={}, json={}", key, json, e);
            return null;
        }
    }
}