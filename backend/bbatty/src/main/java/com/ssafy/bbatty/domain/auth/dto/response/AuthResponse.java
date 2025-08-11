package com.ssafy.bbatty.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증 관련 응답 DTO
 *
 * 🎯 프론트엔드 개발자를 위한 가이드:
 * - tokens: HTTP 요청 시 Authorization 헤더에 사용
 * - userInfo: AsyncStorage에 저장하여 UI 구성에 사용
 */
@Getter
@Builder
public class AuthResponse {

    /**
     * JWT 토큰 쌍
     * 📝 사용법:
     * - accessToken을 Authorization: Bearer {token} 헤더에 포함
     * - refreshToken은 토큰 갱신 시에만 사용
     */
    private final TokenPair tokens;

    /**
     * 사용자 UI 정보
     * 📝 사용법:
     * - AsyncStorage에 저장하여 오프라인에서도 사용
     * - 프로필 화면, 닉네임 표시, 팀 브랜딩 등에 활용
     * - JWT 토큰과는 별개로 관리 (토큰에는 최소 정보만 포함)
     */
    private final UserProfile userProfile;

    @Getter
    @Builder
    public static class UserProfile {
        /**
         * 사용자 고유 ID
         * 🔹 JWT 토큰에도 포함됨 (sub 클레임)
         */
        private final Long userId;

        /**
         * 사용자 닉네임
         * 🔹 JWT 토큰에 포함되지 않음 (크기 최소화)
         * 📱 사용처: 헤더, 댓글 작성자명, 프로필 화면
         */
        private final String nickname;

        /**
         * 프로필 이미지 URL
         * 🔹 JWT 토큰에 포함되지 않음
         * 📱 사용처: 프로필 아바타, 댓글 프로필 이미지
         */
        private final String profileImg;

        /**
         * 응원팀 ID
         * 🔹 JWT 토큰에도 포함됨 (teamId 클레임)
         * 📱 사용처: 팀별 게시판 접근 권한 확인
         */
        private final Long teamId;

        /**
         * 나이
         * 🔹 JWT 토큰에도 포함됨 (age 클레임)
         * 📱 사용처: 필터링
         */
        private final int age;

        /**
         * 성별 ("MALE" 또는 "FEMALE")
         * 🔹 JWT 토큰에도 포함됨 (gender 클레임)
         * 📱 사용처: 필터링
         */
        private final String gender;
    }

    /**
     * 로그인 성공 응답 생성
     * 📝 프론트 처리:
     * 1. tokens를 SecureStore/KeyChain에 안전하게 저장
     * 2. userProfile AsyncStorage에 저장 (빠른 UI 로딩용)
     * 3. 홈 화면으로 네비게이션
     */
    public static AuthResponse ofLogin(TokenPair tokens, UserProfile userProfile) {
        return AuthResponse.builder()
                .tokens(tokens)
                .userProfile(userProfile)
                .build();
    }

    /**
     * 회원가입 성공 응답 생성
     * 📝 프론트 처리:
     * 1. tokens를 SecureStore/KeyChain에 안전하게 저장
     * 2. userProfile AsyncStorage에 저장
     * 3. 온보딩 완료 화면 또는 홈 화면으로 네비게이션
     */
    public static AuthResponse ofSignup(TokenPair tokens, UserProfile userProfile) {
        return AuthResponse.builder()
                .tokens(tokens)
                .userProfile(userProfile)
                .build();
    }
}