package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.dto.internal.AttendanceRecord;
import com.ssafy.schedule.global.util.AttendanceRecordParser;
import com.ssafy.schedule.global.constants.BadgeCategory;
import com.ssafy.schedule.global.constants.BadgeType;
import com.ssafy.schedule.global.constants.RedisKey;
import com.ssafy.schedule.global.constants.Stadium;
import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.global.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 뱃지 업데이트 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeUpdateServiceImpl implements BadgeUpdateService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final GameRepository gameRepository;
    private final AttendanceRecordParser attendanceRecordParser;
    
    @Override
    public void updateBadgesOnGameResult(Long userId, Long gameId, String season) {
        // 입력 값 검증
        if (userId == null || gameId == null || season == null || season.trim().isEmpty()) {
            log.warn("뱃지 업데이트 입력 값 오류: userId={}, gameId={}, season={}", userId, gameId, season);
            return;
        }
        
        log.info("경기 결과 업데이트로 인한 뱃지 업데이트 시작: userId={}, gameId={}, season={}", userId, gameId, season);
        
        boolean stadiumBadgeUpdated = false;
        boolean winBadgeUpdated = false;
        boolean gameBadgeUpdated = false;
        
        try {
            // 1. 경기 정보 조회
            Optional<Game> gameOpt = gameRepository.findById(gameId);
            if (gameOpt.isEmpty()) {
                log.warn("게임을 찾을 수 없어 뱃지 업데이트 중단: gameId={}", gameId);
                return;
            }
            
            Game game = gameOpt.get();
            
            try {
                Stadium stadium = Stadium.findByName(game.getStadium());
                updateStadiumBadge(userId, stadium);
                stadiumBadgeUpdated = true;
            } catch (Exception e) {
                log.warn("구장 뱃지 업데이트 실패: userId={}, stadium={}", userId, game.getStadium(), e);
            }
            
            // 3. 해당 시즌의 사용자 직관 기록 조회
            List<AttendanceRecord> records = getSeasonAttendanceRecords(userId, season, game);
            
            if (records.isEmpty()) {
                log.info("해당 시즌 직관 기록이 없어 시즌 뱃지 업데이트 불가: userId={}, season={}", userId, season);
            } else {
                try {
                    // 4. 승리 수 계산 및 승리 뱃지 업데이트
                    int totalWins = (int) records.stream()
                        .filter(record -> record.getUserGameResult() == AttendanceRecord.UserGameResult.WIN)
                        .count();
                    updateWinBadges(userId, season, totalWins);
                    winBadgeUpdated = true;
                } catch (Exception e) {
                    log.warn("승리 뱃지 업데이트 실패: userId={}, season={}", userId, season, e);
                }
                
                try {
                    // 5. 직관 경기 수 뱃지 업데이트
                    updateGameBadges(userId, season, records.size());
                    gameBadgeUpdated = true;
                } catch (Exception e) {
                    log.warn("경기 수 뱃지 업데이트 실패: userId={}, season={}", userId, season, e);
                }
            }
            
            log.info("뱃지 업데이트 완료: userId={}, stadium={}, win={}, game={}", 
                userId, stadiumBadgeUpdated, winBadgeUpdated, gameBadgeUpdated);
                
        } catch (Exception e) {
            log.error("뱃지 업데이트 전체 실패: userId={}, gameId={}, season={}", userId, gameId, season, e);
        }
    }
    
    @Override
    public void updateStadiumBadge(Long userId, Stadium stadium) {
        try {
            BadgeType stadiumBadge = getStadiumBadgeByStadium(stadium);
            if (stadiumBadge == null) {
                log.warn("해당 구장에 대한 뱃지가 없음: stadium={}", stadium);
                return;
            }
            
            String redisKey = RedisKey.BADGE_STADIUM + userId + ":" + stadium.name();
            String existingTimestamp = (String) redisTemplate.opsForValue().get(redisKey);
            
            if (existingTimestamp == null) {
                // 새로운 구장 뱃지 획득
                String timestamp = LocalDateTime.now().toString();
                redisTemplate.opsForValue().set(redisKey, timestamp);
                log.info("새로운 구장 뱃지 획득: userId={}, stadium={}, badge={}", userId, stadium, stadiumBadge);
            }
        } catch (Exception e) {
            log.error("구장 뱃지 업데이트 중 오류: userId={}, stadium={}", userId, stadium, e);
        }
    }
    
    @Override
    public void updateWinBadges(Long userId, String season, int totalWins) {
        try {
            for (BadgeType badgeType : BadgeType.values()) {
                if (badgeType.getCategory() == BadgeCategory.SEASON_WINS) {
                    Integer requiredWins = (Integer) badgeType.getRequirement();
                    
                    if (totalWins >= requiredWins) {
                        String redisKey = RedisKey.BADGE_WINS + userId + ":" + season + ":" + requiredWins;
                        String existingTimestamp = (String) redisTemplate.opsForValue().get(redisKey);
                        
                        if (existingTimestamp == null) {
                            // 새로운 승리 뱃지 획득
                            String timestamp = LocalDateTime.now().toString();
                            redisTemplate.opsForValue().set(redisKey, timestamp);
                            log.info("새로운 승리 뱃지 획득: userId={}, season={}, badge={}, totalWins={}", 
                                userId, season, badgeType, totalWins);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("승리 뱃지 업데이트 중 오류: userId={}, season={}, totalWins={}", userId, season, totalWins, e);
        }
    }
    
    @Override
    public void updateGameBadges(Long userId, String season, int totalGames) {
        try {
            for (BadgeType badgeType : BadgeType.values()) {
                if (badgeType.getCategory() == BadgeCategory.SEASON_GAMES) {
                    Integer requiredGames = (Integer) badgeType.getRequirement();
                    
                    if (totalGames >= requiredGames) {
                        String redisKey = RedisKey.BADGE_GAMES + userId + ":" + season + ":" + requiredGames;
                        String existingTimestamp = (String) redisTemplate.opsForValue().get(redisKey);
                        
                        if (existingTimestamp == null) {
                            // 새로운 직관 경기 뱃지 획득
                            String timestamp = LocalDateTime.now().toString();
                            redisTemplate.opsForValue().set(redisKey, timestamp);
                            log.info("새로운 직관 경기 뱃지 획득: userId={}, season={}, badge={}, totalGames={}", 
                                userId, season, badgeType, totalGames);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("직관 경기 뱃지 업데이트 중 오류: userId={}, season={}, totalGames={}", userId, season, totalGames, e);
        }
    }
    
    /**
     * 해당 시즌의 사용자 직관 기록 조회
     */
    private List<AttendanceRecord> getSeasonAttendanceRecords(Long userId, String season, Game game) {
        List<AttendanceRecord> records = new ArrayList<>();
        
        // Redis에서 해당 시즌 직관 기록 조회
        String redisKey = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":" + season;
        Set<Object> attendanceRecords = redisTemplate.opsForZSet().reverseRange(redisKey, 0, -1);
        
        if (attendanceRecords == null || attendanceRecords.isEmpty()) {
            return records;
        }
        
        // JSON 레코드들을 AttendanceRecord로 변환
        for (Object recordObj : attendanceRecords) {
            try {
                String recordJson = String.valueOf(recordObj);
                AttendanceRecord record = attendanceRecordParser.parseSimpleAttendanceRecord(recordJson, game);
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
     * 구장에 해당하는 뱃지 타입 조회
     */
    private BadgeType getStadiumBadgeByStadium(Stadium stadium) {
        for (BadgeType badgeType : BadgeType.values()) {
            if (badgeType.isStadiumBadge() && badgeType.getRequirement().equals(stadium)) {
                return badgeType;
            }
        }
        return null;
    }
    
}