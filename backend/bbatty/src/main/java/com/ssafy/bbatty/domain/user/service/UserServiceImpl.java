package com.ssafy.bbatty.domain.user.service;

import com.ssafy.bbatty.domain.board.service.PostService;
import com.ssafy.bbatty.domain.user.dto.request.UserUpdateRequestDto;
import com.ssafy.bbatty.domain.user.dto.response.UserResponseDto;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.RedisKey;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PostService postService;
    private final RedisUtil redisUtil;

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
    public Object getUserStats(Long targetUserId, Long currentUserId, String season, String stadium, 
                              String opponent, String dayOfWeek, String homeAway) {
        // 본인이 아니면 statsPublic 검증
        if (!targetUserId.equals(currentUserId)) {
            User user = findUserById(targetUserId);
            if (!user.getStatsPublic()) {
                throw new ApiException(ErrorCode.PRIVATE_CONTENT_ACCESS_DENIED);
            }
        }
        
        // 레디스에서 사용자 통계 조회
        return getUserStatsFromRedis(targetUserId, season, stadium, opponent, dayOfWeek, homeAway);
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
        return !userRepository.existsByNickname(nickname) || 
               userRepository.findByNickname(nickname)
                   .map(User::getId)
                   .filter(id -> id.equals(currentUserId))
                   .isPresent();
    }

    @Override
    @Transactional
    public void updatePrivacySettings(Long currentUserId, Boolean postsPublic, Boolean statsPublic, Boolean attendanceRecordsPublic) {
        User user = findUserById(currentUserId);
        user.updatePrivacySettings(postsPublic, statsPublic, attendanceRecordsPublic);
    }

    @Override
    @Transactional
    public void deleteUser(Long currentUserId) {
        User user = findUserById(currentUserId);
        user.delete(); // 소프트 딜리트 수행
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 레디스에서 사용자 통계 조회
     */
    private Map<String, Object> getUserStatsFromRedis(Long userId, String season, String stadium, 
                                                     String opponent, String dayOfWeek, String homeAway) {
        // 시즌 기본값 설정 (현재 연도)
        if (season == null) {
            season = String.valueOf(LocalDate.now().getYear());
        }
        
        String key = RedisKey.STATS_USER_DETAILED + userId + ":" + season;
        Map<String, String> allStats = redisUtil.getHashAll(key);
        
        if (allStats.isEmpty()) {
            return Map.of(
                "totalGames", 0,
                "wins", 0,
                "draws", 0,
                "losses", 0,
                "winRate", 0.0
            );
        }
        
        // 필터링 적용
        Map<String, Object> filteredStats = new HashMap<>();
        
        // 전체 통계
        if (stadium == null && opponent == null && dayOfWeek == null && homeAway == null) {
            filteredStats.put("totalGames", Integer.parseInt(allStats.getOrDefault("totalGames", "0")));
            filteredStats.put("wins", Integer.parseInt(allStats.getOrDefault("wins", "0")));
            filteredStats.put("draws", Integer.parseInt(allStats.getOrDefault("draws", "0")));
            filteredStats.put("losses", Integer.parseInt(allStats.getOrDefault("losses", "0")));
            filteredStats.put("winRate", Double.parseDouble(allStats.getOrDefault("winRate", "0.0")));
        } else {
            // 필터링된 통계 (구장별, 상대팀별 등)
            String filterKey = buildFilterKey(stadium, opponent, dayOfWeek, homeAway);
            filteredStats.put("totalGames", Integer.parseInt(allStats.getOrDefault(filterKey + "_games", "0")));
            filteredStats.put("wins", Integer.parseInt(allStats.getOrDefault(filterKey + "_wins", "0")));
            filteredStats.put("draws", Integer.parseInt(allStats.getOrDefault(filterKey + "_draws", "0")));
            filteredStats.put("losses", Integer.parseInt(allStats.getOrDefault(filterKey + "_losses", "0")));
            filteredStats.put("winRate", Double.parseDouble(allStats.getOrDefault(filterKey + "_winRate", "0.0")));
        }
        
        return filteredStats;
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
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
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

    /**
     * 필터 조건으로 키 생성
     */
    private String buildFilterKey(String stadium, String opponent, String dayOfWeek, String homeAway) {
        StringBuilder sb = new StringBuilder();
        
        if (stadium != null) sb.append("stadium_").append(stadium).append("_");
        if (opponent != null) sb.append("opponent_").append(opponent).append("_");
        if (dayOfWeek != null) sb.append("day_").append(dayOfWeek).append("_");
        if (homeAway != null) sb.append("location_").append(homeAway).append("_");
        
        // 마지막 언더스코어 제거
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        
        return sb.toString();
    }
}
