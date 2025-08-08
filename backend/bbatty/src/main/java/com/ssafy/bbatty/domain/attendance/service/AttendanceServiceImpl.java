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
import java.time.ZoneId;
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
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        
        // 2. 사용자 팀의 당일 예정 경기 조회 (캐시 우선)
        List<Game> todayGames = getTeamGamesToday(teamId, today);
        
        if (todayGames.isEmpty()) {
            log.info("직관 인증 실패 - 당일 경기 없음: userId={}, teamId={}", userId, teamId);
            throw new ApiException(ErrorCode.NO_GAME_TODAY);
        }

        // 3. 직관 인증 (시간 + 위치 검증)
        Game verifiedGame = verifyAttendance(todayGames, request);

        // 4. 중복 인증 확인 (DB에서만 확인)
        boolean alreadyAttended = userAttendedRepository.existsByUserIdAndGameId(userId, verifiedGame.getId());
        
        if (alreadyAttended) {
            log.info("직관 인증 실패 - 이미 인증함: userId={}, gameId={}", userId, verifiedGame.getId());
            throw new ApiException(ErrorCode.ALREADY_ATTENDED_GAME);
        }
        
        // 5. 직관 기록 저장
        // DB (로깅용)
        UserAttended userAttended = UserAttended.of(userId, verifiedGame.getId());
        userAttendedRepository.save(userAttended);
        // Redis (통계용)
        saveAttendanceRecordToRedis(userId, verifiedGame);

        // 직관 인증 정보 저장 (직관 채팅 입장용, 당일 자정까지 TTL)
        String attendanceGameKey = RedisKey.USER_ATTENDANCE_GAME + userId + ":" + verifiedGame.getId();
        Duration ttlUntilMidnight = DateUtil.calculateTTLUntilMidnight();
        redisUtil.setValue(attendanceGameKey, "ATTENDED", ttlUntilMidnight);
        
        // 당일 인증자 목록에 추가 (이틀 TTL)
        String dailyAttendeesKey = RedisKey.ATTENDANCE_DAILY_ATTENDEES + today;
        Duration weekTTL = Duration.ofDays(2);
        redisUtil.addToSet(dailyAttendeesKey, userId.toString());
        redisUtil.expire(dailyAttendeesKey, weekTTL);
        
        log.info("Redis에 직관 인증 저장: key={}, gameId={}, TTL={}초", 
                attendanceGameKey, verifiedGame.getId(), ttlUntilMidnight.getSeconds());
        log.info("당일 인증자 목록 업데이트: key={}, userId={}", dailyAttendeesKey, userId);
        
        // 6. 응답 생성
        Stadium stadium = Stadium.findByName(verifiedGame.getStadium());
        LocationUtil.LocationValidationResult locationResult = 
                LocationUtil.validateStadiumLocation(request.latitude(), request.longitude(), stadium);
        
        AttendanceVerifyResponse.GameInfo gameInfo = AttendanceVerifyResponse.GameInfo.builder()
                .gameId(verifiedGame.getId())
                .homeTeam(verifiedGame.getHomeTeam().getName())
                .awayTeam(verifiedGame.getAwayTeam().getName())
                .gameDateTime(verifiedGame.getDateTime())
                .status(verifiedGame.getStatus().name())
                .build();
        
        AttendanceVerifyResponse.StadiumInfo stadiumInfo = AttendanceVerifyResponse.StadiumInfo.builder()
                .stadiumName(locationResult.stadiumName())
                .latitude(locationResult.stadiumLatitude())
                .longitude(locationResult.stadiumLongitude())
                .distanceFromUser(locationResult.getDistanceMeters())
                .build();
        
        String attendanceId = generateAttendanceId(userId, verifiedGame.getId());
        
        log.info("직관 인증 성공 - userId: {}, gameId: {}, stadium: {}", 
                userId, verifiedGame.getId(), verifiedGame.getStadium());
        
        return AttendanceVerifyResponse.success(attendanceId, gameInfo, stadiumInfo);
    }
    
    /**
     * 직관 인증 (시간 + 위치 검증)
     */
    private Game verifyAttendance(List<Game> games, AttendanceVerifyRequest request) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));

        boolean hasTimeValidGame = false;
        boolean hasLocationValidGame = false;
        
        for (Game game : games) {
            // 1. 시간 검증: 경기 시작 2시간 전부터 당일 자정까지
            LocalDateTime gameStart = game.getDateTime();
            LocalDateTime verifyStart = gameStart.minusHours(Attendance.ATTENDANCE_START_HOURS_BEFORE);
            LocalDateTime verifyEnd = gameStart.toLocalDate()
                    .atTime(Attendance.ATTENDANCE_DEADLINE_HOUR, 
                           Attendance.ATTENDANCE_DEADLINE_MINUTE, 
                           Attendance.ATTENDANCE_DEADLINE_SECOND);
            
            boolean timeValid = !now.isBefore(verifyStart) && !now.isAfter(verifyEnd);
            
            if (timeValid) {
                hasTimeValidGame = true;
                
                // 2. (시간 검증 통과 시) 위치 검증: 경기장 150m 내
                try {
                    Stadium stadium = Stadium.findByName(game.getStadium());
                    LocationUtil.LocationValidationResult locationResult = 
                            LocationUtil.validateStadiumLocation(request.latitude(), request.longitude(), stadium);
                    
                    if (locationResult.withinRange()) {
                        hasLocationValidGame = true;
                        return game; // 시간 + 위치 조건 모두 만족
                    } else {
                        log.info("위치 검증 실패 - 경기장에서 {}m 떨어져 있음", locationResult.getDistanceMeters());
                        throw new ApiException(ErrorCode.NOT_IN_STADIUM);
                    }
                } catch (ApiException e) {
                    log.warn("경기장 정보 없음: {}", game.getStadium());
                    continue;
                }
            } else {
                log.info("시간 검증 실패 - 현재: {}, 인증가능시간: {} ~ {}", now, verifyStart, verifyEnd);
                throw new ApiException(ErrorCode.NOT_ATTENDANCE_TIME);
            }
        }
        
        // 일반적인 검증 실패
        throw new ApiException(ErrorCode.ATTENDANCE_VALIDATION_FAILED);
    }
    
    /**
     * 인증 ID 생성
     */
    private String generateAttendanceId(Long userId, Long gameId) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
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
    
    /**
     * 직관 기록을 Redis에 저장 (통계용)
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
            
            log.info("Redis 직관 기록 저장 완료 - userId: {}, gameId: {}, season: {}", 
                    userId, game.getId(), season);
            
        } catch (Exception e) {
            log.error("Redis 직관 기록 저장 실패 - userId: {}, gameId: {}", userId, game.getId(), e);
            // Redis 저장 실패해도 전체 프로세스는 계속 진행
        }
    }
    
    /**
     * 경기 정보를 JSON 문자열로 변환
     */
    private String createGameRecord(Game game) {
        // 간단한 JSON 형태로 생성 (Jackson 라이브러리 없이)
        return String.format(
                "{\"gameId\":%d,\"homeTeam\":\"%s\",\"awayTeam\":\"%s\",\"dateTime\":\"%s\",\"stadium\":\"%s\",\"status\":\"%s\"}",
                game.getId(),
                game.getHomeTeam().getName(),
                game.getAwayTeam().getName(), 
                game.getDateTime().toString(),
                game.getStadium(),
                game.getStatus().name()
        );
    }
    
}
