package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.domain.auth.client.KakaoClient;
import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequest;
import com.ssafy.bbatty.domain.auth.dto.request.SignupRequest;
import com.ssafy.bbatty.domain.auth.dto.response.AuthResponse;
import com.ssafy.bbatty.domain.auth.dto.response.KakaoUserResponse;
import com.ssafy.bbatty.domain.auth.dto.response.TokenPair;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.entity.UserInfo;
import com.ssafy.bbatty.domain.user.repository.UserInfoRepository;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock private KakaoClient kakaoClient;
    @Mock private JwtProvider jwtProvider;
    @Mock private AuthCacheService authCacheService;
    @Mock private UserRepository userRepository;
    @Mock private UserInfoRepository userInfoRepository;
    @Mock private TeamRepository teamRepository;

    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("카카오 로그인 - 기존 사용자 성공")
    void kakaoLogin_ExistingUser_Success() {
        // Given
        KakaoLoginRequest request = new KakaoLoginRequest("valid-kakao-token");
        KakaoUserResponse kakaoUser = createMockKakaoUser();
        User existingUser = createMockUser();
        UserInfo existingUserInfo = createMockUserInfo(existingUser, kakaoUser.getKakaoId());
        TokenPair tokenPair = createMockTokenPair();

        when(kakaoClient.getUserInfo("valid-kakao-token")).thenReturn(kakaoUser);
        when(userInfoRepository.findByKakaoId(kakaoUser.getKakaoId()))
                .thenReturn(Optional.of(existingUserInfo));
        when(jwtProvider.createAccessToken(any(), anyInt(), anyString(), any()))
                .thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProvider.getExpiration("access-token")).thenReturn(new Date());
        when(jwtProvider.getExpiration("refresh-token")).thenReturn(new Date());

        // When
        AuthResponse response = authService.kakaoLogin(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTokens()).isNotNull();
        assertThat(response.getUserInfo()).isNotNull();
        assertThat(response.getUserInfo().getUserId()).isEqualTo(existingUser.getId());
        assertThat(response.getUserInfo().getNickname()).isEqualTo(existingUser.getNickname());

        verify(kakaoClient).getUserInfo("valid-kakao-token");
        verify(userInfoRepository).findByKakaoId(kakaoUser.getKakaoId());
        verify(jwtProvider).createAccessToken(existingUser.getId(), existingUser.getAge(), 
                existingUser.getGender().name(), existingUser.getTeamId());
    }

    @Test
    @DisplayName("카카오 로그인 - 신규 사용자 예외 발생")
    void kakaoLogin_NewUser_ThrowsException() {
        // Given
        KakaoLoginRequest request = new KakaoLoginRequest("valid-kakao-token");
        KakaoUserResponse kakaoUser = createMockKakaoUser();

        when(kakaoClient.getUserInfo("valid-kakao-token")).thenReturn(kakaoUser);
        when(userInfoRepository.findByKakaoId(kakaoUser.getKakaoId()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.kakaoLogin(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(kakaoClient).getUserInfo("valid-kakao-token");
        verify(userInfoRepository).findByKakaoId(kakaoUser.getKakaoId());
        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("회원가입 - 성공")
    void signup_Success() {
        // Given
        SignupRequest request = createSignupRequest();
        KakaoUserResponse kakaoUser = createMockKakaoUser();
        Team team = createMockTeam();
        User savedUser = createMockUser();

        when(kakaoClient.getUserInfo(request.getAccessToken())).thenReturn(kakaoUser);
        when(userInfoRepository.existsByKakaoId(kakaoUser.getKakaoId())).thenReturn(false);
        when(userRepository.existsByNickname(request.getNickname())).thenReturn(false);
        when(teamRepository.findById(request.getTeamId())).thenReturn(Optional.of(team));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userInfoRepository.save(any(UserInfo.class))).thenReturn(mock(UserInfo.class));
        when(jwtProvider.createAccessToken(any(), anyInt(), anyString(), any()))
                .thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProvider.getExpiration("access-token")).thenReturn(new Date());
        when(jwtProvider.getExpiration("refresh-token")).thenReturn(new Date());

        // When
        AuthResponse response = authService.signup(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTokens()).isNotNull();
        assertThat(response.getUserInfo()).isNotNull();

        verify(kakaoClient).getUserInfo(request.getAccessToken());
        verify(userInfoRepository).existsByKakaoId(kakaoUser.getKakaoId());
        verify(userRepository).existsByNickname(request.getNickname());
        verify(teamRepository).findById(request.getTeamId());
        verify(userRepository).save(any(User.class));
        verify(userInfoRepository).save(any(UserInfo.class));
    }

    @Test
    @DisplayName("회원가입 - 중복 가입 예외")
    void signup_DuplicateSignup_ThrowsException() {
        // Given
        SignupRequest request = createSignupRequest();
        KakaoUserResponse kakaoUser = createMockKakaoUser();

        when(kakaoClient.getUserInfo(request.getAccessToken())).thenReturn(kakaoUser);
        when(userInfoRepository.existsByKakaoId(kakaoUser.getKakaoId())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SIGNUP);

        verify(userInfoRepository).existsByKakaoId(kakaoUser.getKakaoId());
        verify(userRepository, never()).existsByNickname(anyString());
    }

    @Test
    @DisplayName("회원가입 - 중복 닉네임 예외")
    void signup_DuplicateNickname_ThrowsException() {
        // Given
        SignupRequest request = createSignupRequest();
        KakaoUserResponse kakaoUser = createMockKakaoUser();

        when(kakaoClient.getUserInfo(request.getAccessToken())).thenReturn(kakaoUser);
        when(userInfoRepository.existsByKakaoId(kakaoUser.getKakaoId())).thenReturn(false);
        when(userRepository.existsByNickname(request.getNickname())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);

        verify(userRepository).existsByNickname(request.getNickname());
        verify(teamRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("토큰 갱신 - 성공")
    void refreshToken_Success() {
        // Given
        String refreshToken = "valid-refresh-token";
        Long userId = 1L;
        User user = createMockUser();

        when(jwtProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(authCacheService.isTokenBlacklisted(refreshToken)).thenReturn(false);
        when(jwtProvider.getUserId(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.getExpiration(refreshToken)).thenReturn(new Date());
        when(jwtProvider.createAccessToken(any(), anyInt(), anyString(), any()))
                .thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtProvider.getExpiration("new-access-token")).thenReturn(new Date());
        when(jwtProvider.getExpiration("new-refresh-token")).thenReturn(new Date());

        // When
        TokenPair result = authService.refreshToken(refreshToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");

        verify(jwtProvider).validateRefreshToken(refreshToken);
        verify(authCacheService).isTokenBlacklisted(refreshToken);
        verify(authCacheService).blacklistToken(eq(refreshToken), any(Date.class));
    }

    @Test
    @DisplayName("토큰 갱신 - 무효한 토큰 예외")
    void refreshToken_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid-token";

        when(jwtProvider.validateRefreshToken(invalidToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);

        verify(jwtProvider).validateRefreshToken(invalidToken);
        verifyNoInteractions(authCacheService);
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_Success() {
        // Given
        String accessToken = "valid-access-token";
        String refreshToken = "valid-refresh-token";

        when(jwtProvider.validateAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getExpiration(accessToken)).thenReturn(new Date());
        when(jwtProvider.getExpiration(refreshToken)).thenReturn(new Date());

        // When
        authService.logout(accessToken, refreshToken);

        // Then
        verify(jwtProvider).validateAccessToken(accessToken);
        verify(jwtProvider).validateRefreshToken(refreshToken);
        verify(authCacheService).blacklistToken(eq(accessToken), any(Date.class));
        verify(authCacheService).blacklistToken(eq(refreshToken), any(Date.class));
    }

    // Helper methods for creating mock objects
    private KakaoUserResponse createMockKakaoUser() {
        KakaoUserResponse mockResponse = mock(KakaoUserResponse.class);
        when(mockResponse.getKakaoId()).thenReturn("12345");
        when(mockResponse.getEmail()).thenReturn("test@kakao.com");
        when(mockResponse.getBirthYear()).thenReturn("1990");
        when(mockResponse.getGender()).thenReturn("male");
        return mockResponse;
    }

    private User createMockUser() {
        Team team = createMockTeam();
        return User.builder()
                .id(1L)
                .team(team)
                .nickname("testUser")
                .gender(Gender.MALE)
                .birthYear(1990)
                .profileImg("profile.jpg")
                .introduction("Hello")
                .build();
    }

    private UserInfo createMockUserInfo(User user, String kakaoId) {
        return UserInfo.builder()
                .id(1L)
                .user(user)
                .kakaoId(kakaoId)
                .email("test@kakao.com")
                .build();
    }

    private Team createMockTeam() {
        return Team.builder()
                .id(1L)
                .name("한화 이글스")
                .build();
    }

    private SignupRequest createSignupRequest() {
        return new SignupRequest(
                "valid-kakao-token",   // accessToken
                "12345",               // kakaoId
                "test@kakao.com",      // email
                "1990",                // birthYear
                "male",                // gender
                1L,                    // teamId
                "newUser",             // nickname
                "profile.jpg",         // profileImg
                "Hello World"          // introduction
        );
    }

    private TokenPair createMockTokenPair() {
        return TokenPair.of(
                "access-token",
                "refresh-token",
                new Date(),
                new Date()
        );
    }
}