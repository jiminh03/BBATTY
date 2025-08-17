package com.ssafy.bbatty.global.util;

import com.ssafy.bbatty.domain.attendance.entity.UserAttended;
import com.ssafy.bbatty.domain.attendance.repository.UserAttendedRepository;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.global.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;

/**
 * 더미데이터 Redis 마이그레이션 유틸리티
 * - DB의 직관 데이터를 Redis로 옮겨서 통계 계산이 가능하게 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceDataMigrationUtil {

    private final UserAttendedRepository userAttendedRepository;
    private final GameRepository gameRepository;
    private final RedisUtil redisUtil;

    /**
     * DB의 모든 직관 기록을 Redis로 마이그레이션
     */
    public void migrateAllAttendanceDataToRedis() {
        log.info("=== DB -> Redis 직관 데이터 마이그레이션 시작 ===");

        List<UserAttended> allAttendances = userAttendedRepository.findAll();
        log.info("마이그레이션할 직관 기록 개수: {}", allAttendances.size());

        int successCount = 0;
        int failCount = 0;

        for (UserAttended attendance : allAttendances) {
            try {
                Game game = gameRepository.findById(attendance.getGameId())
                        .orElse(null);
                
                if (game == null) {
                    log.warn("게임을 찾을 수 없음: gameId={}", attendance.getGameId());
                    failCount++;
                    continue;
                }

                saveAttendanceRecordToRedis(attendance.getUserId(), game);
                successCount++;

                if (successCount % 100 == 0) {
                    log.info("마이그레이션 진행 상황: {}/{}", successCount, allAttendances.size());
                }

            } catch (Exception e) {
                log.error("마이그레이션 실패: userId={}, gameId={}", 
                    attendance.getUserId(), attendance.getGameId(), e);
                failCount++;
            }
        }

        log.info("=== 마이그레이션 완료 ===");
        log.info("성공: {}, 실패: {}, 전체: {}", successCount, failCount, allAttendances.size());
    }

    /**
     * 특정 사용자의 직관 기록만 Redis로 마이그레이션
     */
    public void migrateUserAttendanceDataToRedis(Long userId) {
        log.info("사용자 직관 데이터 마이그레이션 시작: userId={}", userId);

        // 전체 직관 기록 중에서 해당 사용자 것만 필터링
        List<UserAttended> allAttendances = userAttendedRepository.findAll();
        List<UserAttended> userAttendances = allAttendances.stream()
                .filter(attendance -> attendance.getUserId().equals(userId))
                .toList();
                
        log.info("사용자 직관 기록 개수: {}", userAttendances.size());

        int successCount = 0;
        int failCount = 0;

        for (UserAttended attendance : userAttendances) {
            try {
                Game game = gameRepository.findById(attendance.getGameId())
                        .orElse(null);
                
                if (game == null) {
                    log.warn("게임을 찾을 수 없음: gameId={}", attendance.getGameId());
                    failCount++;
                    continue;
                }

                saveAttendanceRecordToRedis(userId, game);
                successCount++;

            } catch (Exception e) {
                log.error("사용자 마이그레이션 실패: userId={}, gameId={}", 
                    userId, attendance.getGameId(), e);
                failCount++;
            }
        }

        log.info("사용자 마이그레이션 완료: userId={}, 성공={}, 실패={}", userId, successCount, failCount);
    }

    /**
     * 직관 기록을 Redis에 저장 (통계용)
     * AttendanceServiceImpl의 saveAttendanceRecordToRedis()와 동일한 로직
     */
    private void saveAttendanceRecordToRedis(Long userId, Game game) {
        try {
            // 경기 시간을 한국 시간 기준 타임스탬프로 변환 (정렬용 스코어)
            long timestamp = game.getDateTime()
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toInstant()
                    .toEpochMilli();

            // 경기 정보 JSON 생성
            String gameRecord = createGameRecord(game);

            // 시즌별 직관 기록 저장
            String season = String.valueOf(game.getDateTime().getYear());
            String seasonKey = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":" + season;
            redisUtil.addToSortedSet(seasonKey, gameRecord, timestamp);

            // 통산 직관 기록 저장
            String totalKey = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":total";
            redisUtil.addToSortedSet(totalKey, gameRecord, timestamp);

            log.debug("Redis 직관 기록 저장 완료 - userId: {}, gameId: {}, season: {}", 
                    userId, game.getId(), season);

        } catch (Exception e) {
            log.error("Redis 직관 기록 저장 실패 - userId: {}, gameId: {}", userId, game.getId(), e);
            throw e; // 마이그레이션 에러로 다시 던짐
        }
    }

    /**
     * Redis에 저장된 사용자 직관 데이터를 디버깅 (문제 진단용)
     */
    public Map<String, Object> debugUserRedisData(Long userId) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            // 1. 2025 시즌 데이터 확인
            String season2025Key = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":2025";
            Set<String> season2025Records = redisUtil.reverseRange(season2025Key, 0, -1);
            debugInfo.put("season_2025_key", season2025Key);
            debugInfo.put("season_2025_count", season2025Records != null ? season2025Records.size() : 0);
            debugInfo.put("season_2025_records", season2025Records != null ? season2025Records : Collections.emptySet());
            
            // 2. total 데이터 확인
            String totalKey = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":total";
            Set<String> totalRecords = redisUtil.reverseRange(totalKey, 0, -1);
            debugInfo.put("total_key", totalKey);
            debugInfo.put("total_count", totalRecords != null ? totalRecords.size() : 0);
            debugInfo.put("total_records", totalRecords != null ? totalRecords : Collections.emptySet());
            
            // 3. DB 데이터 확인
            List<UserAttended> dbRecords = userAttendedRepository.findAll().stream()
                    .filter(attendance -> attendance.getUserId().equals(userId))
                    .toList();
            debugInfo.put("db_count", dbRecords.size());
            
            // 4. 게임 결과 확인 (처음 몇 개 게임의 상세 정보)
            List<Map<String, Object>> gameDetails = new ArrayList<>();
            for (int i = 0; i < Math.min(5, dbRecords.size()); i++) {
                UserAttended attendance = dbRecords.get(i);
                Optional<Game> gameOpt = gameRepository.findById(attendance.getGameId());
                if (gameOpt.isPresent()) {
                    Game game = gameOpt.get();
                    Map<String, Object> gameDetail = new HashMap<>();
                    gameDetail.put("gameId", game.getId());
                    gameDetail.put("homeTeam", game.getHomeTeam().getName());
                    gameDetail.put("awayTeam", game.getAwayTeam().getName());
                    gameDetail.put("gameResult", game.getResult() != null ? game.getResult().name() : "NULL");
                    gameDetail.put("dateTime", game.getDateTime().toString());
                    gameDetail.put("status", game.getStatus().name());
                    gameDetails.add(gameDetail);
                }
            }
            debugInfo.put("sample_games", gameDetails);
            
            log.info("Redis 디버깅 완료: userId={}, 시즌2025={}, 총합={}, DB={}",
                    userId, 
                    season2025Records != null ? season2025Records.size() : 0,
                    totalRecords != null ? totalRecords.size() : 0,
                    dbRecords.size());
                    
        } catch (Exception e) {
            log.error("Redis 디버깅 중 오류: userId={}", userId, e);
            debugInfo.put("error", e.getMessage());
        }
        
        return debugInfo;
    }

    /**
     * 경기 정보를 JSON 문자열로 변환
     * AttendanceServiceImpl의 createGameRecord()와 동일한 로직
     */
    private String createGameRecord(Game game) {
        return String.format(
                "{\"gameId\":%d,\"homeTeam\":\"%s\",\"awayTeam\":\"%s\",\"dateTime\":\"%s\",\"stadium\":\"%s\",\"status\":\"%s\",\"homeScore\":%s,\"awayScore\":%s,\"result\":\"%s\"}",
                game.getId(),
                game.getHomeTeam().getName(),
                game.getAwayTeam().getName(),
                game.getDateTime().toString(),
                game.getStadium(),
                game.getStatus().name(),
                game.getHomeScore() != null ? game.getHomeScore() : "null",
                game.getAwayScore() != null ? game.getAwayScore() : "null",
                game.getResult() != null ? game.getResult().name() : "null"
        );
    }
}