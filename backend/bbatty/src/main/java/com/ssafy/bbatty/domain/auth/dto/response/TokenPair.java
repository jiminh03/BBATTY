package com.ssafy.bbatty.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;

/**
 * JWT 토큰 쌍 DTO
 *
 * 🎯 프론트엔드 개발자를 위한 가이드:
 *
 * 📋 토큰 사용 전략:
 * 1. accessToken: 모든 API 요청의 Authorization 헤더에 포함
 * 2. refreshToken: accessToken 만료 시 갱신용으로만 사용
 * 3. 만료시간을 확인하여 자동 갱신 로직 구현
 *
 * 💾 저장 위치:
 * - React Native: @react-native-async-storage/async-storage 또는 expo-secure-store
 * - 보안이 중요한 토큰이므로 SecureStore 권장
 */
@Getter
@Builder
public class TokenPair {

    /**
     * 액세스 토큰 (수명: 1시간)
     * 📝 사용법:
     * - Authorization: Bearer {accessToken}
     * - 모든 보호된 API 엔드포인트 요청 시 필수
     *
     * 💡 포함된 정보:
     * - userId, age, gender, teamId (서버에서 인증/인가용)
     */
    private final String accessToken;

    /**
     * 리프레시 토큰 (수명: 2주)
     * 📝 사용법:
     * - X-Refresh-Token: {refreshToken} 헤더 또는 body에 포함
     * - POST /auth/refresh 엔드포인트에서만 사용
     * - 새로운 TokenPair를 받으면 기존 토큰들 교체
     */
    private final String refreshToken;

    /**
     * 액세스 토큰 만료 시간
     * 📝 활용법:
     * - API 호출 전 만료 확인
     * - 만료 5분 전에 미리 갱신하여 UX 향상
     */
    private final Date accessTokenExpiresAt;

    /**
     * 리프레시 토큰 만료 시간
     * 📝 활용법:
     * - 리프레시 토큰도 만료되면 재로그인 필요
     * - 만료 임박 시 사용자에게 재로그인 안내
     */
    private final Date refreshTokenExpiresAt;

    /**
     * 토큰 쌍 생성
     * 📝 프론트 수신 후 처리:
     * 1. 두 토큰 모두 안전한 저장소에 저장
     * 2. 만료시간 기반 자동 갱신 타이머 설정
     * 3. HTTP 인터셉터에서 자동으로 Authorization 헤더 추가
     */
    public static TokenPair of(String accessToken, String refreshToken,
                               Date accessTokenExpiresAt, Date refreshTokenExpiresAt) {
        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .build();
    }
}