package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequest;
import com.ssafy.bbatty.domain.auth.dto.request.SignupRequest;
import com.ssafy.bbatty.domain.auth.dto.response.AuthResponse;
import com.ssafy.bbatty.domain.auth.dto.response.TokenPair;

/**
 * 인증 서비스 인터페이스
 */
public interface AuthService {

    /**
     * 카카오 로그인 처리
     */
    AuthResponse kakaoLogin(KakaoLoginRequest request);

    /**
     * 회원가입 처리
     */
    AuthResponse signup(SignupRequest request);

    /**
     * 토큰 갱신 (Stateless 방식)
     */
    TokenPair refreshToken(String refreshToken);

    /**
     * 닉네임 중복 확인
     */
    boolean isNicknameAvailable(String nickname);

    /**
     * 로그아웃
     */
    void logout(String accessToken, String refreshToken);
}