package com.ssafy.bbatty.domain.attendance.service;

import com.ssafy.bbatty.domain.attendance.dto.redis.ActiveGame;
import com.ssafy.bbatty.domain.attendance.dto.request.AttendanceVerifyRequest;
import com.ssafy.bbatty.domain.attendance.dto.response.AttendanceVerifyResponse;
import com.ssafy.bbatty.domain.attendance.entity.UserAttended;
import com.ssafy.bbatty.domain.attendance.repository.UserAttendedRepository;
import com.ssafy.bbatty.domain.attendance.repository.redis.ActiveGameRepository;
import com.ssafy.bbatty.domain.attendance.util.LocationUtil;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.Attendance;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.GameStatus;
import com.ssafy.bbatty.global.constants.RedisKey;
import com.ssafy.bbatty.global.constants.Stadium;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.util.DateUtil;
import com.ssafy.bbatty.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 직관 인증 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {
    
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final UserAttendedRepository userAttendedRepository;
    private final RedisUtil redisUtil;
    private final ActiveGameRepository activeGameRepository;
    
    @Override
    public AttendanceVerifyResponse verifyAttendance(Long userId, AttendanceVerifyRequest request) {
        log.info("직관 인증 시작 - userId: {}, 위치: ({}, {})", userId, request.latitude(), request.longitude());
        
        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        
        Long teamId = user.getTeamId();
        LocalDate today = LocalDate.now();
        
        // 2. 사용자 팀의 당일 예정 경기 조회 (캐시 우선)
        List<Game> todayGames = getTeamGamesToday(teamId, today);
        
        if (todayGames.isEmpty()) {
            log.info("직관 인증 실패 - 당일 경기 없음: userId={}, teamId={}", userId, teamId);
            throw new ApiException(ErrorCode.NO_GAME_TODAY);
        }
        
        // 3. 인증 가능한 경기 찾기 (시간 + 위치 검증) 
        Game eligibleGame = findEligibleGame(todayGames, request);
        
        // 4. 중복 인증 확인 (경기별 개별 검증)
        String redisKey = RedisKey.ATTENDANCE_GAME + eligibleGame.getId() + ":" + userId;
        boolean alreadyAttendedRedis = redisUtil.hasKey(redisKey);
        boolean alreadyAttendedDB = userAttendedRepository.existsByUserIdAndGameId(userId, eligibleGame.getId());
        
        if (alreadyAttendedRedis || alreadyAttendedDB) {
            log.info("직관 인증 실패 - 이미 인증함: userId={}, gameId={}", userId, eligibleGame.getId());
            throw new ApiException(ErrorCode.ALREADY_ATTENDED_GAME);
        }
        
        // 5. 직관 기록 저장 (DB + Redis)
        UserAttended userAttended = UserAttended.of(userId, eligibleGame.getId());
        userAttendedRepository.save(userAttended);
        
        // Redis에 인증 정보 저장 (당일 자정까지 TTL)
        Duration ttlUntilMidnight = DateUtil.calculateTTLUntilMidnight();
        redisUtil.setValue(redisKey, "ATTENDED", ttlUntilMidnight);
        
        // 당일 인증자 목록에 추가 (이틀 TTL)
        String dailyAttendeesKey = RedisKey.ATTENDANCE_DAILY_ATTENDEES + today;
        Duration weekTTL = Duration.ofDays(2);
        redisUtil.addToSet(dailyAttendeesKey, userId.toString());
        redisUtil.expire(dailyAttendeesKey, weekTTL);
        
        log.info("Redis에 직관 인증 저장: key={}, gameId={}, TTL={}초", 
                redisKey, eligibleGame.getId(), ttlUntilMidnight.getSeconds());
        log.info("당일 인증자 목록 업데이트: key={}, userId={}", dailyAttendeesKey, userId);
        
        // 6. 응답 생성
        Stadium stadium = Stadium.findByName(eligibleGame.getStadium());
        LocationUtil.LocationValidationResult locationResult = 
                LocationUtil.validateStadiumLocation(request.latitude(), request.longitude(), stadium);
        
        AttendanceVerifyResponse.GameInfo gameInfo = AttendanceVerifyResponse.GameInfo.builder()
                .gameId(eligibleGame.getId())
                .homeTeam(eligibleGame.getHomeTeam().getName())
                .awayTeam(eligibleGame.getAwayTeam().getName())
                .gameDateTime(eligibleGame.getDateTime())
                .status(eligibleGame.getStatus().name())
                .build();
        
        AttendanceVerifyResponse.StadiumInfo stadiumInfo = AttendanceVerifyResponse.StadiumInfo.builder()
                .stadiumName(locationResult.stadiumName())
                .latitude(locationResult.stadiumLatitude())
                .longitude(locationResult.stadiumLongitude())
                .distanceFromUser(locationResult.getDistanceMeters())
                .build();
        
        String attendanceId = generateAttendanceId(userId, eligibleGame.getId());
        
        log.info("직관 인증 성공 - userId: {}, gameId: {}, stadium: {}", 
                userId, eligibleGame.getId(), eligibleGame.getStadium());
        
        return AttendanceVerifyResponse.success(attendanceId, gameInfo, stadiumInfo);
    }
    
    /**
     * 인증 가능한 경기 찾기 (시간 + 위치 검증)
     */
    private Game findEligibleGame(List<Game> games, AttendanceVerifyRequest request) {
        LocalDateTime now = LocalDateTime.now();
        boolean hasTimeValidGame = false;
        boolean hasLocationValidGame = false;
        
        for (Game game : games) {
            // 시간 검증: 경기 시작 2시간 전부터 당일 자정까지
            LocalDateTime gameStart = game.getDateTime();
            LocalDateTime verifyStart = gameStart.minusHours(Attendance.ATTENDANCE_START_HOURS_BEFORE);
            LocalDateTime verifyEnd = gameStart.toLocalDate()
                    .atTime(Attendance.ATTENDANCE_DEADLINE_HOUR, 
                           Attendance.ATTENDANCE_DEADLINE_MINUTE, 
                           Attendance.ATTENDANCE_DEADLINE_SECOND);
            
            boolean timeValid = !now.isBefore(verifyStart) && !now.isAfter(verifyEnd);
            
            if (timeValid) {
                hasTimeValidGame = true;
                
                // 위치 검증: 경기장 150m 내
                try {
                    Stadium stadium = Stadium.findByName(game.getStadium());
                    LocationUtil.LocationValidationResult locationResult = 
                            LocationUtil.validateStadiumLocation(request.latitude(), request.longitude(), stadium);
                    
                    if (locationResult.withinRange()) {
                        return game; // 시간 + 위치 조건 모두 만족
                    } else {
                        hasLocationValidGame = false;
                        log.info("위치 검증 실패 - 경기장에서 {}m 떨어져 있음", locationResult.getDistanceMeters());
                    }
                } catch (ApiException e) {
                    log.warn("경기장 정보 없음: {}", game.getStadium());
                    continue;
                }
            } else {
                log.info("시간 검증 실패 - 현재: {}, 인증가능시간: {} ~ {}", now, verifyStart, verifyEnd);
            }
        }
        
        // 구체적인 실패 원인에 따른 에러 코드 반환
        if (!hasTimeValidGame) {
            throw new ApiException(ErrorCode.NOT_ATTENDANCE_TIME);
        }
        if (hasTimeValidGame && !hasLocationValidGame) {
            throw new ApiException(ErrorCode.NOT_IN_STADIUM);
        }
        
        // 일반적인 검증 실패
        throw new ApiException(ErrorCode.ATTENDANCE_VALIDATION_FAILED);
    }
    
    /**
     * 인증 ID 생성
     */
    private String generateAttendanceId(Long userId, Long gameId) {
        LocalDate today = LocalDate.now();
        return String.format("%s_USER%d_GAME%d", 
                today.toString().replace("-", ""), userId, gameId);
    }
    
    /**
     * 팀의 당일 경기 조회 (캐시 우선, DB 백업)
     */
    private List<Game> getTeamGamesToday(Long teamId, LocalDate today) {
        // 1. Redis 캐시에서 조회 시도
        Optional<List<ActiveGame>> cachedGames = activeGameRepository.findTeamGamesToday(teamId, today);
        if (cachedGames.isPresent()) {
            log.info("캐시에서 경기 조회 성공 - Redis Hit: teamId={}, count={}", teamId, cachedGames.get().size());
            return convertToGameList(cachedGames.get());
        }
        
        // 2. DB에서 조회 (캐시 미스)
        List<Game> dbGames = gameRepository.findTeamGamesToday(teamId, today, GameStatus.SCHEDULED);
        log.info("DB에서 경기 조회 - Redis Miss: teamId={}, count={}", teamId, dbGames.size());
        
        // 3. 조회 결과를 캐시에 저장
        if (!dbGames.isEmpty()) {
            List<ActiveGame> activeGames = convertToActiveGameList(dbGames);
            activeGameRepository.cacheTeamGamesToday(teamId, today, activeGames);
        }
        
        return dbGames;
    }
    
    /**
     * Game -> ActiveGame 변환 (구장 위치 정보 포함)
     */
    private List<ActiveGame> convertToActiveGameList(List<Game> games) {
        return games.stream()
                .map(game -> {
                    Stadium stadium = Stadium.findByName(game.getStadium());
                    return ActiveGame.builder()
                            .gameId(game.getId())
                            .homeTeam(game.getHomeTeam().getName())
                            .awayTeam(game.getAwayTeam().getName())
                            .gameDateTime(game.getDateTime())
                            .stadium(game.getStadium())
                            .status(game.getStatus().name())
                            .stadiumLatitude(stadium.getLatitude())
                            .stadiumLongitude(stadium.getLongitude())
                            .stadiumDisplayName(stadium.getStadiumName())
                            .build();
                })
                .toList();
    }
    
    /**
     * ActiveGame -> Game 변환 (필요시 DB 재조회)
     */
    private List<Game> convertToGameList(List<ActiveGame> activeGames) {
        List<Long> gameIds = activeGames.stream()
                .map(ActiveGame::getGameId)
                .toList();
        
        return gameRepository.findAllById(gameIds);
    }
    
}
