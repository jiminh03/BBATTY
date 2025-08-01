package com.ssafy.bbatty.global.security;

import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtProvider 단위 테스트")
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private final String secret = "this-is-a-very-long-secret-key-for-jwt-token-generation-and-validation-test";
    private final String issuer = "bbatty-test";
    private final long accessTokenValidityInHours = 1;
    private final long refreshTokenValidityInDays = 14;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                secret,
                issuer,
                accessTokenValidityInHours,
                refreshTokenValidityInDays
        );
    }

    @Test
    @DisplayName("Access Token 생성 - 성공")
    void createAccessToken_Success() {
        // Given
        Long userId = 1L;
        int age = 30;
        String gender = "MALE";
        Long teamId = 1L;

        // When
        String accessToken = jwtProvider.createAccessToken(userId, age, gender, teamId);

        // Then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();

        // 토큰 파싱하여 클레임 검증
        Claims claims = jwtProvider.getClaims(accessToken);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer()).isEqualTo(issuer);
        assertThat(claims.get("age", Integer.class)).isEqualTo(age);
        assertThat(claims.get("gender", String.class)).isEqualTo(gender);
        assertThat(claims.get("teamId", Long.class)).isEqualTo(teamId);
        assertThat(claims.get("tokenType")).isNull(); // Access Token에는 tokenType 없음
    }

    @Test
    @DisplayName("Refresh Token 생성 - 성공")
    void createRefreshToken_Success() {
        // Given
        Long userId = 1L;

        // When
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();

        // 토큰 파싱하여 클레임 검증
        Claims claims = jwtProvider.getClaims(refreshToken);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer()).isEqualTo(issuer);
        assertThat(claims.get("tokenType", String.class)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("토큰에서 사용자 ID 추출 - 성공")
    void getUserId_Success() {
        // Given
        Long userId = 123L;
        String token = jwtProvider.createAccessToken(userId, 25, "FEMALE", 2L);

        // When
        Long extractedUserId = jwtProvider.getUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("토큰에서 나이 추출 - 성공")
    void getAge_Success() {
        // Given
        int age = 28;
        String token = jwtProvider.createAccessToken(1L, age, "MALE", 1L);

        // When
        int extractedAge = jwtProvider.getAge(token);

        // Then
        assertThat(extractedAge).isEqualTo(age);
    }

    @Test
    @DisplayName("토큰에서 성별 추출 - 성공")
    void getGender_Success() {
        // Given
        String gender = "FEMALE";
        String token = jwtProvider.createAccessToken(1L, 25, gender, 1L);

        // When
        String extractedGender = jwtProvider.getGender(token);

        // Then
        assertThat(extractedGender).isEqualTo(gender);
    }

    @Test
    @DisplayName("토큰에서 팀 ID 추출 - 성공")
    void getTeamId_Success() {
        // Given
        Long teamId = 5L;
        String token = jwtProvider.createAccessToken(1L, 30, "MALE", teamId);

        // When
        Long extractedTeamId = jwtProvider.getTeamId(token);

        // Then
        assertThat(extractedTeamId).isEqualTo(teamId);
    }

    @Test
    @DisplayName("Access Token 검증 - 유효한 토큰")
    void validateAccessToken_ValidToken_ReturnsTrue() {
        // Given
        String accessToken = jwtProvider.createAccessToken(1L, 30, "MALE", 1L);

        // When
        boolean isValid = jwtProvider.validateAccessToken(accessToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Access Token 검증 - Refresh Token 전달시 실패")
    void validateAccessToken_RefreshToken_ReturnsFalse() {
        // Given
        String refreshToken = jwtProvider.createRefreshToken(1L);

        // When
        boolean isValid = jwtProvider.validateAccessToken(refreshToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Refresh Token 검증 - 유효한 토큰")
    void validateRefreshToken_ValidToken_ReturnsTrue() {
        // Given
        String refreshToken = jwtProvider.createRefreshToken(1L);

        // When
        boolean isValid = jwtProvider.validateRefreshToken(refreshToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Refresh Token 검증 - Access Token 전달시 실패")
    void validateRefreshToken_AccessToken_ReturnsFalse() {
        // Given
        String accessToken = jwtProvider.createAccessToken(1L, 30, "MALE", 1L);

        // When
        boolean isValid = jwtProvider.validateRefreshToken(accessToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("토큰 만료 시간 추출 - 성공")
    void getExpiration_Success() {
        // Given
        String token = jwtProvider.createAccessToken(1L, 30, "MALE", 1L);

        // When
        Date expiration = jwtProvider.getExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration.after(new Date())).isTrue(); // 현재 시간보다 이후
    }

    @Test
    @DisplayName("Bearer 토큰 추출 - 성공")
    void extractToken_Success() {
        // Given
        String bearerToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        // When
        String extractedToken = jwtProvider.extractToken(bearerToken);

        // Then
        assertThat(extractedToken).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
    }

    @Test
    @DisplayName("Bearer 토큰 추출 - Bearer 접두사 없음")
    void extractToken_NoBearerPrefix_ReturnsNull() {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        // When
        String extractedToken = jwtProvider.extractToken(token);

        // Then
        assertThat(extractedToken).isNull();
    }

    @Test
    @DisplayName("Bearer 토큰 추출 - null 입력")
    void extractToken_NullInput_ReturnsNull() {
        // When
        String extractedToken = jwtProvider.extractToken(null);

        // Then
        assertThat(extractedToken).isNull();
    }

    @Test
    @DisplayName("만료된 토큰 파싱 - 예외 발생")
    void getClaims_ExpiredToken_ThrowsException() {
        // Given - 만료된 토큰 생성 (과거 시간으로 설정)
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
        
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuer(issuer)
                .issuedAt(Date.from(pastTime.minus(1, ChronoUnit.HOURS)))
                .expiration(Date.from(pastTime)) // 1시간 전에 만료
                .signWith(secretKey)
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtProvider.getClaims(expiredToken))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("잘못된 서명 토큰 파싱 - 예외 발생")
    void getClaims_InvalidSignature_ThrowsException() {
        // Given - 다른 시크릿으로 서명된 토큰
        String differentSecret = "different-secret-key-for-testing-invalid-signature";
        SecretKey differentSecretKey = Keys.hmacShaKeyFor(differentSecret.getBytes());
        
        String invalidToken = Jwts.builder()
                .subject("1")
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(differentSecretKey) // 다른 키로 서명
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtProvider.getClaims(invalidToken))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("잘못된 형식 토큰 파싱 - 예외 발생")
    void getClaims_MalformedToken_ThrowsException() {
        // Given
        String malformedToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtProvider.getClaims(malformedToken))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("빈 토큰 파싱 - 예외 발생")
    void getClaims_EmptyToken_ThrowsException() {
        // Given
        String emptyToken = "";

        // When & Then
        assertThatThrownBy(() -> jwtProvider.getClaims(emptyToken))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("null 토큰 파싱 - 예외 발생")
    void getClaims_NullToken_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> jwtProvider.getClaims(null))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }
}