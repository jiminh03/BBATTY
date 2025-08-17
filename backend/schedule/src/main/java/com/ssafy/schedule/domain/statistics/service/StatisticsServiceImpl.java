package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.dto.internal.AttendanceRecord;
import com.ssafy.schedule.domain.statistics.dto.response.UserBasicStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserDetailedStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserStreakStatsResponse;
import com.ssafy.schedule.domain.statistics.repository.StatisticsRedisRepository;
import com.ssafy.schedule.global.util.AttendanceRecordParser;
import com.ssafy.schedule.global.constants.RedisKey;
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
    private final AttendanceRecordParser attendanceRecordParser;

    @Override
    public UserBasicStatsResponse calculateUserBasicStats(Long userId, String season, Long teamId) {
        log.info("사용자 기본 통계 계산 시작: userId={}, season={}, teamId={}", userId, season, teamId);

        // 캐시 확인 (시즌별 키 구분)
        Object cached = statisticsRedisRepository.getUserWinRate(userId, season);
        if (cached instanceof UserBasicStatsResponse) {
            return (UserBasicStatsResponse) cached;
        }

        // 선택 시즌 직관 기록 조회 및 계산
        List<AttendanceRecord> records = getAttendanceRecords(userId, season, teamId);

        UserBasicStatsResponse response = buildBasicStats(userId, season, records);

        // 캐시 저장
        statisticsRedisRepository.saveUserWinRate(userId, season, response);


        log.info("사용자 기본 통계 계산 완료: userId={}, season={}, winRate={}", userId, season, response.getWinRate());
        return response;
    }

    @Override
    public UserDetailedStatsResponse calculateUserDetailedStats(Long userId, String season, Long teamId) {
        log.info("사용자 상세 통계 계산 시작: userId={}, season={}, teamId={}", userId, season, teamId);

        // 캐시 확인
        Object cached = statisticsRedisRepository.getUserDetailedStats(userId, season);
        if (cached instanceof UserDetailedStatsResponse) {
            return (UserDetailedStatsResponse) cached;
        }

        // 직관 기록 조회 및 계산
        List<AttendanceRecord> records = getAttendanceRecords(userId, season, teamId);

        UserDetailedStatsResponse response = buildDetailedStats(userId, season, records);

        // 캐시 저장
        statisticsRedisRepository.saveUserDetailedStats(userId, season, response);

        log.info("사용자 상세 통계 계산 완료: userId={}, season={}", userId, season);
        return response;
    }

    @Override
    public UserStreakStatsResponse calculateUserStreakStats(Long userId, String season, Long teamId) {
        log.info("사용자 연승 통계 계산 시작: userId={}, season={}, teamId={}", userId, season, teamId);

        // 캐시 확인 (시즌별로 캐시)
        Object cached = statisticsRedisRepository.getUserStreakStats(userId, season);
        if (cached instanceof UserStreakStatsResponse) {
            return (UserStreakStatsResponse) cached;
        }

        // 통산 직관 기록 조회 (연승 계산을 위해)
        List<AttendanceRecord> allRecords = getAttendanceRecords(userId, "total", teamId);
        
        // 선택 시즌 직관 기록 조회 (승무패 정보를 위해)
        List<AttendanceRecord> seasonRecords = getAttendanceRecords(userId, season, teamId);

        UserStreakStatsResponse response = buildStreakStats(userId, season, allRecords, seasonRecords);

        // 캐시 저장
        statisticsRedisRepository.saveUserStreakStats(userId, season, response);

        log.info("사용자 연승 통계 계산 완료: userId={}, season={}, currentStreak={}", userId, season, response.getCurrentWinStreak());
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
    private List<AttendanceRecord> getAttendanceRecords(Long userId, String season, Long teamId) {
        List<AttendanceRecord> records = new ArrayList<>();

        if ("total".equals(season)) {
            // total인 경우 모든 시즌의 데이터를 합쳐서 조회
            Set<String> allKeys = redisTemplate.keys(RedisKey.USER_ATTENDANCE_RECORDS + userId + ":*");
            if (allKeys != null) {
                for (String key : allKeys) {
                    // total 키는 제외하고 실제 시즌 키들만 처리
                    if (!key.endsWith(":total")) {
                        Set<Object> seasonRecords = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
                        if (seasonRecords != null) {
                            for (Object recordObj : seasonRecords) {
                                try {
                                    String recordJson = String.valueOf(recordObj);
                                    AttendanceRecord record = attendanceRecordParser.parseAttendanceRecord(recordJson, userId, teamId);
                                    if (record != null) {
                                        records.add(record);
                                    }
                                } catch (Exception e) {
                                    log.warn("직관 기록 파싱 실패: userId={}, record={}", userId, recordObj, e);
                                }
                            }
                        }
                    }
                }
            }
            log.info("Redis에서 통산 직관 기록 조회: userId={}, count={}", userId, records.size());
        } else {
            // 특정 시즌인 경우 해당 시즌 키로만 조회
            String redisKey = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":" + season;
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
                    AttendanceRecord record = attendanceRecordParser.parseAttendanceRecord(recordJson, userId, teamId);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    log.warn("직관 기록 파싱 실패: userId={}, record={}", userId, recordObj, e);
                }
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

            // 상대팀별 통계 (팀 ID 사용) - 자기 팀은 제외
            Long opponentTeamId = record.getOpponentTeamId(record.getUserTeamId());
            if (opponentTeamId != null && !opponentTeamId.equals(record.getUserTeamId())) {
                updateCounters(opponentCounters, String.valueOf(opponentTeamId), record.getUserGameResult());
            }

            // 홈/원정별 통계
            if (record.isHomeGame(record.getUserTeamId())) {
                updateCounters(homeCounters, record.getUserGameResult());
            } else {
                updateCounters(awayCounters, record.getUserGameResult());
            }

            // 요일별 통계
            updateCounters(dayOfWeekCounters, record.getDayOfWeek().toString(), record.getUserGameResult());
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
    private UserStreakStatsResponse buildStreakStats(Long userId, String season, List<AttendanceRecord> allRecords, List<AttendanceRecord> seasonRecords) {
        String currentSeason = getCurrentSeason();

        // 연승 계산을 위해 시간 순서대로 정렬 (오래된 것부터)
        allRecords.sort((r1, r2) -> r1.getGameDateTime().compareTo(r2.getGameDateTime()));

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

        // 현재 연승 기록 설정 - 현재 시즌일 때만 실제 진행 중인 연승 기록을 보여줌
        if (season.equals(currentSeason)) {
            currentWinStreak = tempStreak; // 현재 진행 중인 연승 기록
        } else {
            currentWinStreak = 0; // 과거 시즌이나 통산일 때는 0
        }

        // 선택 시즌의 승무패 정보 계산
        int seasonWins = 0;
        int seasonDraws = 0;
        int seasonLosses = 0;
        
        for (AttendanceRecord record : seasonRecords) {
            switch (record.getUserGameResult()) {
                case WIN -> seasonWins++;
                case DRAW -> seasonDraws++;
                case LOSS -> seasonLosses++;
            }
        }

        // 선택 시즌에 따른 최장 연승 기록 설정
        int selectedSeasonMaxStreak = 0;
        if (season.equals("total")) {
            selectedSeasonMaxStreak = maxWinStreakAll; // 통산 최장 연승
        } else {
            selectedSeasonMaxStreak = maxWinStreakBySeason.getOrDefault(season, 0); // 해당 시즌 최장 연승
        }

        return UserStreakStatsResponse.builder()
                .userId(userId)
                .currentSeason(season)
                .currentWinStreak(currentWinStreak)
                .maxWinStreakAll(maxWinStreakAll)
                .maxWinStreakCurrentSeason(selectedSeasonMaxStreak) // 선택한 시즌의 최장 연승으로 변경
                .maxWinStreakBySeason(maxWinStreakBySeason)
                .totalGames(seasonRecords.size())
                .wins(seasonWins)
                .draws(seasonDraws)
                .losses(seasonLosses)
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

}