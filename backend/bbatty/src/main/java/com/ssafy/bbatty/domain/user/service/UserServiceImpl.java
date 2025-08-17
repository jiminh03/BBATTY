package com.ssafy.bbatty.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.attendance.repository.UserAttendedRepository;
import com.ssafy.bbatty.domain.board.service.PostService;
import com.ssafy.bbatty.domain.notification.repository.NotificationSettingRepository;
import com.ssafy.bbatty.domain.user.dto.request.UserUpdateRequestDto;
import com.ssafy.bbatty.domain.user.dto.response.BadgeCategoryResponse;
import com.ssafy.bbatty.domain.user.dto.response.BadgeResponse;
import com.ssafy.bbatty.domain.user.dto.response.UserBadgeResponse;
import com.ssafy.bbatty.domain.user.dto.response.UserResponseDto;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserInfoRepository;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.*;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PostService postService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    
    private final UserInfoRepository userInfoRepository;
    private final UserAttendedRepository userAttendedRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    public UserResponseDto getUserProfile(Long targetUserId, Long currentUserId) {
        User user = findUserById(targetUserId);
        
        // 다른 사용자가 조회하는 경우도 프로필 헤더는 항상 공개
        boolean isOwnProfile = targetUserId.equals(currentUserId);
        
        return UserResponseDto.from(user, isOwnProfile);
    }

    @Override
    public Object getUserPosts(Long targetUserId, Long currentUserId, Long cursor) {
        // 본인이 아니면 postsPublic 검증
        if (!targetUserId.equals(currentUserId)) {
            User user = findUserById(targetUserId);
            if (!user.getPostsPublic()) {
                throw new ApiException(ErrorCode.PRIVATE_CONTENT_ACCESS_DENIED);
            }
        }
        
        // PostService를 사용해서 사용자별 게시글 목록 조회
        return postService.getPostListByUser(targetUserId, cursor);
    }

    @Override
    public Object getUserStats(Long targetUserId, Long currentUserId, String season, String type) {
        // 본인이 아니면 statsPublic 검증
        if (!targetUserId.equals(currentUserId)) {
            User user = findUserById(targetUserId);
            if (!user.getStatsPublic()) {
                throw new ApiException(ErrorCode.PRIVATE_CONTENT_ACCESS_DENIED);
            }
        }
        
        // 레디스에서 사용자 통계 조회
        return getUserStatsFromRedis(targetUserId, season, type);
    }

    @Override
    public Object getUserAttendanceRecords(Long targetUserId, Long currentUserId, String season, Long cursor) {
        // 본인이 아니면 attendanceRecordsPublic 검증
        if (!targetUserId.equals(currentUserId)) {
            User user = findUserById(targetUserId);
            if (!user.getAttendanceRecordsPublic()) {
                throw new ApiException(ErrorCode.PRIVATE_CONTENT_ACCESS_DENIED);
            }
        }
        
        // 레디스에서 사용자 직관 기록 조회
        return getUserAttendanceRecordsFromRedis(targetUserId, season, cursor);
    }

    @Override
    @Transactional
    public UserResponseDto updateProfile(Long currentUserId, UserUpdateRequestDto request) {
        User user = findUserById(currentUserId);
        
        // 닉네임 변경 시 중복 체크
        if (!user.getNickname().equals(request.getNickname()) && 
            !isNicknameAvailable(request.getNickname(), currentUserId)) {
            throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
        }
        
        user.updateProfile(request.getNickname(), request.getIntroduction(), request.getProfileImg());
        
        return UserResponseDto.from(user, true);
    }

    @Override
    public boolean isNicknameAvailable(String nickname, Long currentUserId) {
        User currentUser = findUserById(currentUserId);
        
        // 현재 자신의 닉네임과 동일한 경우 사용 불가 (변경할 필요 없음)
        if (currentUser.getNickname().equals(nickname)) {
            return false;
        }
        
        // 다른 사용자가 사용중인 닉네임인지 확인
        return !userRepository.existsByNickname(nickname);
    }

    @Override
    @Transactional
    public void updatePrivacySettings(Long currentUserId, Boolean postsPublic, Boolean statsPublic, Boolean attendanceRecordsPublic) {
        User user = findUserById(currentUserId);
        user.updatePrivacySettings(postsPublic, statsPublic, attendanceRecordsPublic);
    }

    /**
     * 알림 설정 업데이트
     */
    @Override
    @Transactional
    public void updateNotificationSettings(Long currentUserId, Boolean trafficSpikeAlertEnabled) {
        User user = findUserById(currentUserId);
        user.updateNotificationSettings(trafficSpikeAlertEnabled);

        log.info("알림 설정 업데이트 완료 - userId: {}, trafficSpikeAlertEnabled: {}",
                currentUserId, trafficSpikeAlertEnabled);
    }

    @Override
    @Transactional
    public void deleteUser(Long currentUserId) {
        User user = findUserById(currentUserId);
        
        // 1. 개인정보 하드 삭제 (GDPR, 개인정보보호법 준수)
        deletePersonalInformation(currentUserId);
        
        // 2. 사용자 소프트 삭제 (게시물, 댓글은 "탈퇴한 사용자"로 표시됨)
        user.delete();
        
        // 3. Redis 캐시 데이터 정리
        clearUserDataFromRedis(currentUserId);
        
        log.info("회원 탈퇴 처리 완료: userId={} (개인정보 완전 삭제)", currentUserId);
    }
    
    /**
     * 개인정보 하드 삭제 (GDPR, 개인정보보호법 준수)
     * - UserInfo: 카카오 ID, 이메일 등 개인식별정보
     * - UserAttended: 개인의 직관 활동 기록
     * - NotificationSetting: FCM 토큰, 알림 설정 등 개인정보
     */
    @Transactional
    public void deletePersonalInformation(Long userId) {
        try {
            // UserInfo 하드 삭제 (카카오 ID, 이메일 등 개인식별정보)
            userInfoRepository.deleteByUserId(userId);
            log.info("UserInfo 삭제 완료: userId={}", userId);
            
            // UserAttended 하드 삭제 (개인의 직관 기록)
            userAttendedRepository.deleteByUserId(userId);
            log.info("UserAttended 삭제 완료: userId={}", userId);
            
            // NotificationSetting 하드 삭제 (FCM 토큰, 알림 설정)
            notificationSettingRepository.deleteByUserId(userId);
            log.info("NotificationSetting 삭제 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("개인정보 삭제 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
            throw new ApiException(ErrorCode.USER_DELETION_FAILED);
        }
    }
    
    /**
     * Redis에서 사용자 관련 데이터 정리
     */
    private void clearUserDataFromRedis(Long userId) {
        try {
            // 모든 시즌의 사용자 통계 삭제
            String currentYear = String.valueOf(LocalDate.now().getYear());
            for (int year = 2020; year <= Integer.parseInt(currentYear); year++) {
                String season = String.valueOf(year);
                
                // 기본 통계
                redisUtil.deleteValue(RedisKey.STATS_USER_WINRATE + userId + ":" + season);
                
                // 상세 통계
                redisUtil.deleteValue(RedisKey.STATS_USER_DETAILED + userId + ":" + season);
                
                // 연승 통계
                redisUtil.deleteValue(RedisKey.STATS_USER_STREAK + userId + ":" + season);
                
                // 직관 기록
                redisUtil.deleteValue(RedisKey.USER_ATTENDANCE_RECORDS + userId + ":" + season);
            }
            
            // 뱃지 데이터 삭제 (구장별)
            Arrays.stream(Stadium.values()).forEach(stadium -> {
                redisUtil.deleteValue(RedisKey.BADGE_STADIUM + userId + ":" + stadium.name());
            });
            
            // 시즌별 뱃지 삭제 (승리/게임)
            for (int year = 2020; year <= Integer.parseInt(currentYear); year++) {
                String season = String.valueOf(year);
                
                // 승리 뱃지 - 패턴 삭제
                redisUtil.deleteByPattern(RedisKey.BADGE_WINS + userId + ":" + season + ":*");
                
                // 게임 뱃지 - 패턴 삭제
                redisUtil.deleteByPattern(RedisKey.BADGE_GAMES + userId + ":" + season + ":*");
            }
            
            log.info("Redis 사용자 데이터 정리 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.warn("Redis 사용자 데이터 정리 중 오류 발생: userId={}, error={}", userId, e.getMessage());
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 레디스에서 사용자 통계 조회
     */
    private Object getUserStatsFromRedis(Long userId, String season, String type) {
        // 시즌 기본값 설정 (현재 연도)
        if (season == null) {
            season = String.valueOf(LocalDate.now().getYear());
        }
        
        // type 기본값 설정
        if (type == null) {
            type = "basic";
        }

        return switch (type) {
            case "basic" -> getUserBasicStatsFromRedis(userId, season);
            case "streak" -> getUserStreakStatsFromRedis(userId, season);
            case "stadium" -> getCategoryStatsFromRedis(userId, season, "stadiumStats");
            case "opponent" -> getCategoryStatsFromRedis(userId, season, "opponentStats");
            case "dayOfWeek" -> getCategoryStatsFromRedis(userId, season, "dayOfWeekStats");
            case "homeAway" -> getHomeAwayStatsFromRedis(userId, season);
            default -> throw new ApiException(ErrorCode.BAD_REQUEST);
        };
    }
    
    /**
     * 기본 통계 조회
     */
    private Object getUserBasicStatsFromRedis(Long userId, String season) {
        String key = RedisKey.STATS_USER_WINRATE + userId + ":" + season;
        Object cached = redisUtil.getValue(key, Object.class);
        
        if (cached == null) {
            return Map.of(
                "userId", userId,
                "season", season,
                "totalGames", 0,
                "wins", 0,
                "draws", 0,
                "losses", 0,
                "winRate", "0.000"
            );
        }
        
        return cached;
    }
    
    /**
     * 특정 카테고리 통계 조회 (구장별, 상대팀별, 요일별)
     */
    private Object getCategoryStatsFromRedis(Long userId, String season, String categoryKey) {
        String key = RedisKey.STATS_USER_DETAILED + userId + ":" + season;
        Object cached = redisUtil.getValue(key, Object.class);
        
        if (cached == null) {
            return Map.of(categoryKey, Map.of());
        }
        
        // UserDetailedStatsResponse에서 해당 카테고리만 추출
        if (cached instanceof Map) {
            Map<String, Object> detailedStats = (Map<String, Object>) cached;
            Object categoryStats = detailedStats.get(categoryKey);
            return Map.of(categoryKey, categoryStats != null ? categoryStats : Map.of());
        }
        
        return Map.of(categoryKey, Map.of());
    }
    
    /**
     * 홈/원정 통계 조회
     */
    private Object getHomeAwayStatsFromRedis(Long userId, String season) {
        String key = RedisKey.STATS_USER_DETAILED + userId + ":" + season;
        Object cached = redisUtil.getValue(key, Object.class);
        
        if (cached == null) {
            return Map.of(
                "homeStats", Map.of(),
                "awayStats", Map.of()
            );
        }
        
        // UserDetailedStatsResponse에서 홈/원정 통계만 추출
        if (cached instanceof Map) {
            Map<String, Object> detailedStats = (Map<String, Object>) cached;
            Object homeStats = detailedStats.get("homeStats");
            Object awayStats = detailedStats.get("awayStats");
            return Map.of(
                "homeStats", homeStats != null ? homeStats : Map.of(),
                "awayStats", awayStats != null ? awayStats : Map.of()
            );
        }
        
        return Map.of(
            "homeStats", Map.of(),
            "awayStats", Map.of()
        );
    }
    
    /**
     * 연승 통계 조회
     */
    private Object getUserStreakStatsFromRedis(Long userId, String season) {
        String key = RedisKey.STATS_USER_STREAK + userId + ":" + season;
        Object cached = redisUtil.getValue(key, Object.class);
        
        if (cached == null) {
            return Map.of(
                "userId", userId,
                "currentSeason", season,
                "currentWinStreak", 0,
                "maxWinStreakAll", 0,
                "maxWinStreakCurrentSeason", 0,
                "maxWinStreakBySeason", Map.of(),
                "totalGames", 0,
                "wins", 0,
                "draws", 0,
                "losses", 0
            );
        }
        
        return cached;
    }

    /**
     * 레디스에서 사용자 직관 기록 조회 (커서 기반 페이지네이션)
     */
    private Map<String, Object> getUserAttendanceRecordsFromRedis(Long userId, String season, Long cursor) {
        // 시즌 기본값 설정 (현재 연도)
        if (season == null) {
            season = String.valueOf(LocalDate.now().getYear());
        }
        
        String key = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":" + season;
        
        // Sorted Set에서 커서 기반으로 조회 (score는 타임스탬프)
        Set<String> records;
        if (cursor == null) {
            // 첫 페이지: 최신 20개
            records = redisUtil.reverseRange(key, 0, 19);
        } else {
            // 다음 페이지: cursor보다 작은 score 중 20개
            records = redisUtil.reverseRangeByScore(key, 0, cursor - 1, 20);
        }
        
        // JSON 문자열을 객체로 파싱
        List<Object> parsedRecords = records.stream()
            .map(recordJson -> {
                try {
                    return objectMapper.readValue(recordJson, Map.class);
                } catch (Exception e) {
                    log.warn("직관 기록 JSON 파싱 실패: {}", recordJson);
                    return recordJson; // 파싱 실패 시 원본 문자열 반환
                }
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", parsedRecords);
        result.put("hasMore", records.size() == 20);
        
        // 다음 페이지를 위한 cursor 설정
        if (!records.isEmpty() && records.size() == 20) {
            String lastRecord = records.stream().skip(19).findFirst().orElse(null);
            if (lastRecord != null) {
                Long lastScore = redisUtil.getScore(key, lastRecord);
                result.put("nextCursor", lastScore);
            }
        }

        return result;
    }


    @Override
    public Object getUserAttendanceYears(Long targetUserId, Long currentUserId) {
        // 본인이 아니면 attendanceRecordsPublic 검증
        if (!targetUserId.equals(currentUserId)) {
            User user = findUserById(targetUserId);
            if (!user.getAttendanceRecordsPublic()) {
                throw new ApiException(ErrorCode.PRIVATE_CONTENT_ACCESS_DENIED);
            }
        }

        // Redis에서 사용자 직관 년도 목록 조회
        return getUserAttendanceYearsFromRedis(targetUserId);
    }

    /**
     * Redis에서 사용자 직관 년도 목록 조회
     */
    private Object getUserAttendanceYearsFromRedis(Long userId) {
        List<String> availableYears = new ArrayList<>();
        int totalRecordsCount = 0;

        // 2020년부터 현재 년도까지 Redis 키 존재 여부 확인
        int currentYear = LocalDate.now().getYear();
        for (int year = 2020; year <= currentYear; year++) {
            String seasonKey = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":" + year;

            // Sorted Set에 데이터가 있는지 확인 (1개만 조회해서 존재 여부 확인)
            Set<String> testRecords = redisUtil.reverseRange(seasonKey, 0, 0);
            if (testRecords != null && !testRecords.isEmpty()) {
                availableYears.add(String.valueOf(year));
            }
        }

        // total 키 확인
        String totalKey = RedisKey.USER_ATTENDANCE_RECORDS + userId + ":total";
        Set<String> totalRecords = redisUtil.reverseRange(totalKey, 0, 0);
        boolean hasTotal = totalRecords != null && !totalRecords.isEmpty();

        // total 키가 있으면 전체 레코드 수 조회 (처음 100개만 확인)
        if (hasTotal) {
            Set<String> sampleRecords = redisUtil.reverseRange(totalKey, 0, 99);
            totalRecordsCount = sampleRecords != null ? sampleRecords.size() : 0;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("availableYears", availableYears);
        result.put("hasTotal", hasTotal);
        result.put("sampleTotalRecords", totalRecordsCount);

        return result;
    }

    @Override
    public UserBadgeResponse getUserBadges(Long targetUserId, String season) {
        // 시즌 기본값 설정 (현재 연도)
        if (season == null) {
            season = String.valueOf(LocalDate.now().getYear());
        }

        List<BadgeCategoryResponse> badgeCategories = new ArrayList<>();
        
        // 1. 구장 정복 뱃지 (영구)
        List<BadgeResponse> stadiumBadges = getStadiumBadges(targetUserId);
        badgeCategories.add(BadgeCategoryResponse.stadium(stadiumBadges));
        
        // 2. 시즌 승리 뱃지
        List<BadgeResponse> winBadges = getSeasonBadges(targetUserId, season, BadgeCategory.SEASON_WINS);
        badgeCategories.add(BadgeCategoryResponse.seasonal(BadgeCategory.SEASON_WINS, season, winBadges));
        
        // 3. 시즌 직관 뱃지 
        List<BadgeResponse> gameBadges = getSeasonBadges(targetUserId, season, BadgeCategory.SEASON_GAMES);
        badgeCategories.add(BadgeCategoryResponse.seasonal(BadgeCategory.SEASON_GAMES, season, gameBadges));
        
        return UserBadgeResponse.of(targetUserId, badgeCategories);
    }
    
    /**
     * 구장 뱃지 조회
     */
    private List<BadgeResponse> getStadiumBadges(Long userId) {
        return Arrays.stream(BadgeType.values())
            .filter(BadgeType::isStadiumBadge)
            .map(badgeType -> {
                Stadium stadium = (Stadium) badgeType.getRequirement();
                String redisKey = RedisKey.BADGE_STADIUM + userId + ":" + stadium.name();
                String timestamp = redisUtil.getValue(redisKey, String.class);
                
                return timestamp != null 
                    ? BadgeResponse.acquired(badgeType, LocalDateTime.parse(timestamp))
                    : BadgeResponse.notAcquired(badgeType);
            })
            .collect(Collectors.toList());
    }

    /**
     * 시즌별 뱃지 조회 (승리/직관)
     */
    private List<BadgeResponse> getSeasonBadges(Long userId, String season, BadgeCategory category) {
        return Arrays.stream(BadgeType.values())
            .filter(badgeType -> badgeType.getCategory() == category)
            .map(badgeType -> {
                Integer requirement = (Integer) badgeType.getRequirement();
                String redisKey = (category == BadgeCategory.SEASON_WINS) 
                    ? RedisKey.BADGE_WINS + userId + ":" + season + ":" + requirement
                    : RedisKey.BADGE_GAMES + userId + ":" + season + ":" + requirement;
                    
                String timestamp = redisUtil.getValue(redisKey, String.class);
                
                return timestamp != null 
                    ? BadgeResponse.acquired(badgeType, LocalDateTime.parse(timestamp))
                    : BadgeResponse.notAcquired(badgeType);
            })
            .collect(Collectors.toList());
    }
}