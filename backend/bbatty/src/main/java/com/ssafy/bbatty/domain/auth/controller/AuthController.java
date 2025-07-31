package com.ssafy.bbatty.domain.auth.controller;

import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequest;
import com.ssafy.bbatty.domain.auth.dto.request.SignupRequest;
import com.ssafy.bbatty.domain.auth.dto.response.AuthResponse;
import com.ssafy.bbatty.domain.auth.dto.response.TokenPair;
import com.ssafy.bbatty.domain.auth.service.AuthService;
import com.ssafy.bbatty.global.constants.SuccessCode;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * - 카카오 로그인/회원가입
 * - 토큰 갱신
 * - 로그아웃
 *
 * 📱 프론트엔드 개발자를 위한 API 가이드:
 * 1. POST /auth/kakao/login - 카카오 로그인 (기존 사용자)
 * 2. POST /auth/signup - 회원가입 (신규 사용자)
 * 3. POST /auth/refresh - 토큰 갱신
 * 4. POST /auth/logout - 로그아웃
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    /**
     * 카카오 로그인
     *
     * 📝 프론트 처리:
     * - 성공: AuthResponse 받아서 토큰 저장 후 홈 화면
     * - 실패(USER_NOT_FOUND): 회원가입 화면으로 이동
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request
    ) {
        AuthResponse response = authService.kakaoLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 회원가입
     *
     * 📝 프론트 처리:
     * - 카카오 토큰 + 추가 정보(팀, 닉네임 등) 전송
     * - 성공: AuthResponse 받아서 토큰 저장 후 온보딩 완료
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(SuccessCode.SUCCESS_CREATED, response));
    }

    /**
     * 토큰 갱신
     *
     * 📝 프론트 처리:
     * - X-Refresh-Token 헤더에 리프레시 토큰 포함
     * - 새로운 TokenPair 받아서 기존 토큰 교체
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenPair>> refreshToken(
            HttpServletRequest request
    ) {
        String refreshToken = extractRefreshToken(request);
        TokenPair newTokens = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(newTokens));
    }

    /**
     * 로그아웃
     *
     * 📝 프론트 처리:
     * - Authorization과 X-Refresh-Token 헤더 모두 포함
     * - 성공 후 모든 토큰 삭제하고 로그인 화면으로
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request
    ) {
        String accessToken = jwtProvider.extractToken(request.getHeader("Authorization"));
        String refreshToken = extractRefreshToken(request);

        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 리프레시 토큰 추출 (헤더 또는 파라미터)
     */
    private String extractRefreshToken(HttpServletRequest request) {
        String refreshToken = request.getHeader("X-Refresh-Token");
        if (refreshToken == null) {
            refreshToken = request.getParameter("refreshToken");
        }
        return refreshToken;
    }
}