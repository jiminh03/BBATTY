package com.ssafy.bbatty.domain.user.service;

import com.ssafy.bbatty.domain.board.service.PostService;
import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;
import com.ssafy.bbatty.domain.user.dto.request.UserUpdateRequestDto;
import com.ssafy.bbatty.domain.user.dto.response.UserResponseDto;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.constants.Role;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.util.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserServiceImpl 테스트")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostService postService;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User createTestUser(Long id, String nickname) {
        Team team = Team.builder()
                .id(1L)
                .name("LG 트윈스")
                .build();

        return User.builder()
                .id(id)
                .nickname(nickname)
                .team(team)
                .gender(Gender.MALE)
                .birthYear(1990)
                .profileImg("profile.jpg")
                .introduction("안녕하세요")
                .role(Role.USER)
                .postsPublic(true)
                .statsPublic(true)
                .attendanceRecordsPublic(true)
                .build();
    }

    @Test
    @DisplayName("사용자 프로필 조회 - 본인 프로필")
    void getUserProfile_OwnProfile_Success() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, "테스트유저");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserResponseDto result = userService.getUserProfile(userId, userId);

        // then
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getNickname()).isEqualTo("테스트유저");
        assertThat(result.getProfileImg()).isEqualTo("profile.jpg");
        assertThat(result.getIntroduction()).isEqualTo("안녕하세요");
        assertThat(result.getPostsPublic()).isTrue();
        assertThat(result.getStatsPublic()).isTrue();
        assertThat(result.getAttendanceRecordsPublic()).isTrue();
    }

    @Test
    @DisplayName("사용자 프로필 조회 - 다른 사용자 프로필")
    void getUserProfile_OtherProfile_Success() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        User user = createTestUser(targetUserId, "테스트유저");
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(user));

        // when
        UserResponseDto result = userService.getUserProfile(targetUserId, currentUserId);

        // then
        assertThat(result.getId()).isEqualTo(targetUserId);
        assertThat(result.getNickname()).isEqualTo("테스트유저");
        assertThat(result.getPostsPublic()).isNull(); // 다른 사용자 프로필이므로 프라이버시 설정 미포함
        assertThat(result.getStatsPublic()).isNull();
        assertThat(result.getAttendanceRecordsPublic()).isNull();
    }

    @Test
    @DisplayName("사용자 프로필 조회 - 존재하지 않는 사용자")
    void getUserProfile_UserNotFound_ThrowException() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserProfile(userId, userId))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 게시글 조회 - 본인 게시글")
    void getUserPosts_OwnPosts_Success() {
        // given
        Long userId = 1L;
        Long cursor = 0L;
        PostListPageResponse expectedPosts = new PostListPageResponse();
        given(postService.getPostListByUser(userId, cursor)).willReturn(expectedPosts);

        // when
        Object result = userService.getUserPosts(userId, userId, cursor);

        // then
        assertThat(result).isEqualTo(expectedPosts);
        verify(postService).getPostListByUser(userId, cursor);
    }

    @Test
    @DisplayName("사용자 게시글 조회 - 다른 사용자 공개 게시글")
    void getUserPosts_OtherPublicPosts_Success() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        Long cursor = 0L;
        User user = createTestUser(targetUserId, "테스트유저");
        user.updatePrivacySettings(true, true, true); // 게시글 공개
        
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(user));
        PostListPageResponse expectedPosts = new PostListPageResponse();
        given(postService.getPostListByUser(targetUserId, cursor)).willReturn(expectedPosts);

        // when
        Object result = userService.getUserPosts(targetUserId, currentUserId, cursor);

        // then
        assertThat(result).isEqualTo(expectedPosts);
    }

    @Test
    @DisplayName("사용자 게시글 조회 - 다른 사용자 비공개 게시글")
    void getUserPosts_OtherPrivatePosts_ThrowException() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        Long cursor = 0L;
        User user = createTestUser(targetUserId, "테스트유저");
        user.updatePrivacySettings(false, true, true); // 게시글 비공개
        
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.getUserPosts(targetUserId, currentUserId, cursor))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRIVATE_CONTENT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("사용자 통계 조회 - 본인 통계")
    void getUserStats_OwnStats_Success() {
        // given
        Long userId = 1L;
        String season = "2024";
        String type = "basic";
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("userId", userId);
        mockStats.put("season", season);
        mockStats.put("totalGames", 10);
        mockStats.put("wins", 7);
        mockStats.put("draws", 1);
        mockStats.put("losses", 2);
        mockStats.put("winRate", "0.700");
        
        given(redisUtil.getValue(any(), eq(Object.class))).willReturn(mockStats);

        // when
        Object result = userService.getUserStats(userId, userId, season, type);

        // then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> statsMap = (Map<String, Object>) result;
        assertThat(statsMap.get("totalGames")).isEqualTo(10);
        assertThat(statsMap.get("wins")).isEqualTo(7);
        assertThat(statsMap.get("winRate")).isEqualTo("0.700");
    }

    @Test
    @DisplayName("사용자 통계 조회 - 다른 사용자 비공개 통계")
    void getUserStats_OtherPrivateStats_ThrowException() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        User user = createTestUser(targetUserId, "테스트유저");
        user.updatePrivacySettings(true, false, true); // 통계 비공개
        
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.getUserStats(targetUserId, currentUserId, "2024", ""))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRIVATE_CONTENT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("사용자 직관 기록 조회 - 다른 사용자 비공개 기록")
    void getUserAttendanceRecords_OtherPrivateRecords_ThrowException() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        User user = createTestUser(targetUserId, "테스트유저");
        user.updatePrivacySettings(true, true, false); // 직관 기록 비공개
        
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.getUserAttendanceRecords(targetUserId, currentUserId, "2024", 0L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRIVATE_CONTENT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("프로필 수정 - 성공")
    void updateProfile_Success() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, "기존닉네임");
        UserUpdateRequestDto request = new UserUpdateRequestDto("새닉네임", "new_profile.jpg", "새로운 소개");
        
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);

        // when
        UserResponseDto result = userService.updateProfile(userId, request);

        // then
        assertThat(result.getNickname()).isEqualTo("새닉네임");
        assertThat(result.getIntroduction()).isEqualTo("새로운 소개");
        assertThat(result.getProfileImg()).isEqualTo("new_profile.jpg");
    }

    @Test
    @DisplayName("프로필 수정 - 닉네임 중복")
    void updateProfile_DuplicateNickname_ThrowException() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, "기존닉네임");
        UserUpdateRequestDto request = new UserUpdateRequestDto("중복닉네임", "new_profile.jpg", "새로운 소개");
        
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);
        given(userRepository.findByNickname("중복닉네임")).willReturn(Optional.of(createTestUser(2L, "중복닉네임")));

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("닉네임 중복 체크 - 사용 가능한 닉네임")
    void isNicknameAvailable_AvailableNickname_ReturnTrue() {
        // given
        String nickname = "사용가능닉네임";
        Long userId = 1L;
        User user = createTestUser(userId, "기존닉네임");
        
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname(nickname)).willReturn(false);

        // when
        boolean result = userService.isNicknameAvailable(nickname, userId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("닉네임 중복 체크 - 본인 닉네임")
    void isNicknameAvailable_OwnNickname_ReturnFalse() {
        // given
        String nickname = "본인닉네임";
        Long userId = 1L;
        User user = createTestUser(userId, nickname);
        
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        boolean result = userService.isNicknameAvailable(nickname, userId);

        // then
        assertThat(result).isFalse(); // 자신의 현재 닉네임과 동일하므로 변경 불필요
    }

    @Test
    @DisplayName("닉네임 중복 체크 - 다른 사용자가 사용 중인 닉네임")
    void isNicknameAvailable_TakenNickname_ReturnFalse() {
        // given
        String nickname = "사용중닉네임";
        Long userId = 1L;
        User user = createTestUser(userId, "내닉네임");
        
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname(nickname)).willReturn(true);

        // when
        boolean result = userService.isNicknameAvailable(nickname, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("프라이버시 설정 업데이트 - 성공")
    void updatePrivacySettings_Success() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, "테스트유저");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.updatePrivacySettings(userId, false, false, false);

        // then
        assertThat(user.getPostsPublic()).isFalse();
        assertThat(user.getStatsPublic()).isFalse();
        assertThat(user.getAttendanceRecordsPublic()).isFalse();
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void deleteUser_Success() {
        // given
        Long userId = 1L;
        User user = createTestUser(userId, "테스트유저");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        assertThatCode(() -> userService.deleteUser(userId))
                .doesNotThrowAnyException();

        // then
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
    void findUserById_UserNotFound_ThrowException() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updatePrivacySettings(userId, true, true, true))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}