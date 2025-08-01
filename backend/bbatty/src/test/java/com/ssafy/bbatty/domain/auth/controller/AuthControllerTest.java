package com.ssafy.bbatty.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequest;
import com.ssafy.bbatty.domain.auth.dto.request.SignupRequest;
import com.ssafy.bbatty.domain.auth.dto.response.AuthResponse;
import com.ssafy.bbatty.domain.auth.dto.response.TokenPair;
import com.ssafy.bbatty.domain.auth.service.AuthService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.exception.GlobalExceptionHandler;
import com.ssafy.bbatty.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler()) // 글로벌 예외 핸들러 추가
                .build();
    }

    @Test
    @DisplayName("카카오 로그인 - 성공")
    void kakaoLogin_Success() throws Exception {
        // Given
        KakaoLoginRequest request = new KakaoLoginRequest("valid-kakao-token");
        AuthResponse authResponse = createMockAuthResponse();

        when(authService.kakaoLogin(any(KakaoLoginRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tokens.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.userInfo.userId").value(1))
                .andExpect(jsonPath("$.data.userInfo.nickname").value("testUser"));

        verify(authService).kakaoLogin(any(KakaoLoginRequest.class));
    }

    @Test
    @DisplayName("카카오 로그인 - 사용자 없음 예외")
    void kakaoLogin_UserNotFound_ThrowsException() throws Exception {
        // Given
        KakaoLoginRequest request = new KakaoLoginRequest("valid-kakao-token");

        when(authService.kakaoLogin(any(KakaoLoginRequest.class)))
                .thenThrow(new ApiException(ErrorCode.USER_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        verify(authService).kakaoLogin(any(KakaoLoginRequest.class));
    }

    @Test
    @DisplayName("카카오 로그인 - 유효성 검증 실패")
    void kakaoLogin_InvalidRequest_BadRequest() throws Exception {
        // Given
        KakaoLoginRequest request = new KakaoLoginRequest(""); // 빈 토큰

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("회원가입 - 성공")
    void signup_Success() throws Exception {
        // Given
        SignupRequest request = createSignupRequest();
        AuthResponse authResponse = createMockAuthResponse();

        when(authService.signup(any(SignupRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tokens.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.userInfo.nickname").value("testUser"));

        verify(authService).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 - 중복 가입 예외")
    void signup_DuplicateSignup_ThrowsException() throws Exception {
        // Given
        SignupRequest request = createSignupRequest();

        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new ApiException(ErrorCode.DUPLICATE_SIGNUP));

        // When & Then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()) // DUPLICATE_SIGNUP은 409 Conflict
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_SIGNUP"));

        verify(authService).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("토큰 갱신 - 성공")
    void refreshToken_Success() throws Exception {
        // Given
        TokenPair tokenPair = createMockTokenPair();
        when(authService.refreshToken("valid-refresh-token")).thenReturn(tokenPair);

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                        .header("X-Refresh-Token", "valid-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));

        verify(authService).refreshToken("valid-refresh-token");
    }

    @Test
    @DisplayName("토큰 갱신 - 리프레시 토큰 없음")
    void refreshToken_MissingRefreshToken_ThrowsException() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isNotFound()) // REFRESH_TOKEN_MISSING은 404
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_MISSING"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("토큰 갱신 - 무효한 토큰")
    void refreshToken_InvalidToken_ThrowsException() throws Exception {
        // Given
        when(authService.refreshToken("invalid-token"))
                .thenThrow(new ApiException(ErrorCode.INVALID_TOKEN));

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                        .header("X-Refresh-Token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verify(authService).refreshToken("invalid-token");
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_Success() throws Exception {
        // Given
        when(jwtProvider.extractToken("Bearer access-token")).thenReturn("access-token");
        doNothing().when(authService).logout("access-token", "refresh-token");

        // When & Then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .header("X-Refresh-Token", "refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(jwtProvider).extractToken("Bearer access-token");
        verify(authService).logout("access-token", "refresh-token");
    }

    @Test
    @DisplayName("로그아웃 - 리프레시 토큰 없음")
    void logout_MissingRefreshToken_ThrowsException() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNotFound()) // REFRESH_TOKEN_MISSING은 404
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_MISSING"));

        verifyNoInteractions(authService);
    }

    // Helper methods
    private AuthResponse createMockAuthResponse() {
        TokenPair tokenPair = TokenPair.of(
                "access-token",
                "refresh-token",
                new Date(),
                new Date()
        );
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .userId(1L)
                .nickname("testUser")
                .profileImg("profile.jpg")
                .teamId(1L)
                .teamName("한화 이글스")
                .introduction("Hello")
                .age(34)
                .gender("MALE")
                .build();

        return AuthResponse.builder()
                .tokens(tokenPair)
                .userInfo(userInfo)
                .build();
    }

    private TokenPair createMockTokenPair() {
        return TokenPair.of(
                "new-access-token",
                "new-refresh-token",
                new Date(),
                new Date()
        );
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
}