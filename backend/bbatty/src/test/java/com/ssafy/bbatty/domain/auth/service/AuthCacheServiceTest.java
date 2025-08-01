package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.global.constants.RedisKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthCacheService 단위 테스트")
class AuthCacheServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks private AuthCacheService authCacheService;

    // 테스트용 해시 함수 (실제 구현과 동일)
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다");
        }
    }

    @Test
    @DisplayName("토큰 블랙리스트 추가 - 성공")
    void blacklistToken_Success() {
        // Given
        String token = "sample-jwt-token";
        Date expiration = new Date(System.currentTimeMillis() + 3600000); // 1시간 후

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token, expiration);

        // Then
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(expectedKey), eq("blacklisted"), any(Duration.class));
    }

    @Test
    @DisplayName("토큰 블랙리스트 추가 - 이미 만료된 토큰 (TTL 0 이하)")
    void blacklistToken_ExpiredToken_DoesNotAdd() {
        // Given
        String token = "expired-token";
        Date expiration = new Date(System.currentTimeMillis() - 3600000); // 1시간 전 (이미 만료)

        // When
        authCacheService.blacklistToken(token, expiration);

        // Then
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - 블랙리스트에 있음")
    void isTokenBlacklisted_TokenExists_ReturnsTrue() {
        // Given
        String token = "blacklisted-token";
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);

        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        // When
        boolean result = authCacheService.isTokenBlacklisted(token);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - 블랙리스트에 없음")
    void isTokenBlacklisted_TokenNotExists_ReturnsFalse() {
        // Given
        String token = "valid-token";
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);

        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

        // When
        boolean result = authCacheService.isTokenBlacklisted(token);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - Redis 연결 실패 시 false 반환 (Graceful Degradation)")
    void isTokenBlacklisted_RedisConnectionFailure_ReturnsFalse() {
        // Given
        String token = "token-with-redis-error";
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);

        when(redisTemplate.hasKey(expectedKey)).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = authCacheService.isTokenBlacklisted(token);

        // Then - 현재 서비스 구현에서는 예외를 잡아서 false를 반환함
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("동일한 토큰의 해시값 일관성 검증")
    void hashToken_SameToken_SameHash() {
        // Given
        String token1 = "same-token";
        String token2 = "same-token";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token1, expiration);
        authCacheService.blacklistToken(token2, expiration);

        // Then - 같은 키로 두 번 호출되어야 함 (SHA-256은 동일한 입력에 동일한 출력)
        verify(valueOperations, times(2)).set(eq(expectedKey), eq("blacklisted"), any(Duration.class));
    }

    @Test
    @DisplayName("서로 다른 토큰의 해시값 구분 검증")
    void hashToken_DifferentTokens_DifferentHashes() {
        // Given
        String token1 = "token-one";
        String token2 = "token-two";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token1, expiration);
        authCacheService.blacklistToken(token2, expiration);

        // Then - 서로 다른 토큰은 서로 다른 해시값을 가져야 함
        String expectedKey1 = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token1);
        String expectedKey2 = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token2);

        verify(valueOperations).set(eq(expectedKey1), eq("blacklisted"), any(Duration.class));
        verify(valueOperations).set(eq(expectedKey2), eq("blacklisted"), any(Duration.class));

        // 해시값이 실제로 다른지 확인
        assertThat(hashToken(token1)).isNotEqualTo(hashToken(token2));
    }

    @Test
    @DisplayName("TTL 계산 정확성 검증")
    void blacklistToken_TTLCalculation_Accurate() {
        // Given
        String token = "token-for-ttl-test";
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 7200000; // 2시간 후
        Date expiration = new Date(expirationTime);
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token, expiration);

        // Then
        verify(valueOperations).set(eq(expectedKey), eq("blacklisted"), argThat(duration -> {
            long expectedTtl = expirationTime - currentTime;
            long actualTtl = duration.toMillis();
            // TTL이 예상값의 ±1초 범위 내에 있는지 확인 (시간 차이 보정)
            return Math.abs(actualTtl - expectedTtl) < 1000;
        }));
    }

    @Test
    @DisplayName("null 토큰 처리 - Graceful Degradation")
    void blacklistToken_NullToken_HandlesGracefully() {
        // Given
        String token = null;
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        // When & Then - 현재 서비스 구현에서는 예외를 잡아서 로그만 남기고 계속 진행함
        assertThatCode(() -> authCacheService.blacklistToken(token, expiration))
                .doesNotThrowAnyException();

        // Redis 호출이 발생하지 않음을 확인
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("헬스체크 - Redis 연결 성공")
    void checkRedisConnection_Success() {
        // Given
        when(redisTemplate.hasKey("health-check")).thenReturn(true);

        // When & Then - 예외가 발생하지 않아야 함
        assertThatCode(() -> authCacheService.checkRedisConnection())
                .doesNotThrowAnyException();

        verify(redisTemplate).hasKey("health-check");
    }

    @Test
    @DisplayName("헬스체크 - Redis 연결 실패시 표준 에러 코드 반환")
    void checkRedisConnection_ConnectionFailure_ThrowsStandardError() {
        // Given
        when(redisTemplate.hasKey("health-check"))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Redis connection failed"));

        // When & Then
        assertThatThrownBy(() -> authCacheService.checkRedisConnection())
                .isInstanceOf(com.ssafy.bbatty.global.exception.ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.ssafy.bbatty.global.constants.ErrorCode.REDIS_CONNECTION_FAILED);

        verify(redisTemplate).hasKey("health-check");
    }

    @Test
    @DisplayName("헬스체크 - 예상치 못한 Redis 오류시 표준 에러 코드 반환")
    void checkRedisConnection_UnexpectedError_ThrowsStandardError() {
        // Given
        when(redisTemplate.hasKey("health-check"))
                .thenThrow(new RuntimeException("Unexpected Redis error"));

        // When & Then
        assertThatThrownBy(() -> authCacheService.checkRedisConnection())
                .isInstanceOf(com.ssafy.bbatty.global.exception.ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.ssafy.bbatty.global.constants.ErrorCode.REDIS_OPERATION_FAILED);

        verify(redisTemplate).hasKey("health-check");
    }

    @Test
    @DisplayName("Redis 연결 실패 시 false 반환 (RedisConnectionFailureException)")
    void isTokenBlacklisted_RedisConnectionFailureException_ReturnsFalse() {
        // Given
        String token = "token-with-redis-connection-error";
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);

        when(redisTemplate.hasKey(expectedKey)).thenThrow(new RedisConnectionFailureException("Redis connection failed"));

        // When
        boolean result = authCacheService.isTokenBlacklisted(token);

        // Then - Redis 연결 오류 시 false 반환 (보수적 접근)
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("블랙리스트 추가 시 Redis 연결 실패 - Graceful Degradation")
    void blacklistToken_RedisConnectionFailure_ContinuesGracefully() {
        // Given
        String token = "token-with-redis-error";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis connection failed"));

        // When & Then - 예외가 발생하지 않고 정상적으로 처리되어야 함
        assertThatCode(() -> authCacheService.blacklistToken(token, expiration))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 토큰 처리")
    void blacklistToken_EmptyToken_HandlesGracefully() {
        // Given
        String token = "";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token, expiration);

        // Then
        verify(valueOperations).set(eq(expectedKey), eq("blacklisted"), any(Duration.class));
    }
}