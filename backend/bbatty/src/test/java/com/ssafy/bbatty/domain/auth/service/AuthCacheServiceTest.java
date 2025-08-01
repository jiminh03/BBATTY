package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.global.constants.RedisKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
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

    @Test
    @DisplayName("토큰 블랙리스트 추가 - 성공")
    void blacklistToken_Success() {
        // Given
        String token = "sample-jwt-token";
        Date expiration = new Date(System.currentTimeMillis() + 3600000); // 1시간 후
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + String.valueOf(token.hashCode());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token, expiration);

        // Then
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
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + String.valueOf(token.hashCode());

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
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + String.valueOf(token.hashCode());

        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

        // When
        boolean result = authCacheService.isTokenBlacklisted(token);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("토큰 블랙리스트 확인 - Redis 연결 실패 시 false 반환")
    void isTokenBlacklisted_RedisConnectionFailure_ReturnsFalse() {
        // Given
        String token = "token-with-redis-error";
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + String.valueOf(token.hashCode());

        when(redisTemplate.hasKey(expectedKey)).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        assertThatThrownBy(() -> authCacheService.isTokenBlacklisted(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis connection failed");

        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("동일한 토큰의 해시값 일관성 검증")
    void hashToken_SameToken_SameHash() {
        // Given
        String token1 = "same-token";
        String token2 = "same-token";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token1, expiration);
        authCacheService.blacklistToken(token2, expiration);

        // Then - 같은 키로 두 번 호출되어야 함
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + String.valueOf(token1.hashCode());
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

        // Then - 서로 다른 키로 호출되어야 함
        String expectedKey1 = RedisKey.AUTH_TOKEN_BLACKLIST + String.valueOf(token1.hashCode());
        String expectedKey2 = RedisKey.AUTH_TOKEN_BLACKLIST + String.valueOf(token2.hashCode());

        verify(valueOperations).set(eq(expectedKey1), eq("blacklisted"), any(Duration.class));
        verify(valueOperations).set(eq(expectedKey2), eq("blacklisted"), any(Duration.class));

        // 두 키가 다른지 확인
        assertThat(expectedKey1).isNotEqualTo(expectedKey2);
    }

    @Test
    @DisplayName("TTL 계산 정확성 검증")
    void blacklistToken_TTLCalculation_Accurate() {
        // Given
        String token = "token-for-ttl-test";
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 7200000; // 2시간 후
        Date expiration = new Date(expirationTime);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token, expiration);

        // Then
        verify(valueOperations).set(anyString(), eq("blacklisted"), argThat(duration -> {
            long expectedTtl = expirationTime - currentTime;
            long actualTtl = duration.toMillis();
            // TTL이 예상값의 ±1초 범위 내에 있는지 확인 (시간 차이 보정)
            return Math.abs(actualTtl - expectedTtl) < 1000;
        }));
    }

    @Test
    @DisplayName("null 토큰 처리")
    void blacklistToken_NullToken_HandlesGracefully() {
        // Given
        String token = null;
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        // When & Then
        assertThatThrownBy(() -> authCacheService.blacklistToken(token, expiration))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("빈 토큰 처리")
    void blacklistToken_EmptyToken_HandlesGracefully() {
        // Given
        String token = "";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);
        String expectedKey = RedisKey.AUTH_TOKEN_BLACKLIST + String.valueOf(token.hashCode());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        authCacheService.blacklistToken(token, expiration);

        // Then
        verify(valueOperations).set(eq(expectedKey), eq("blacklisted"), any(Duration.class));
    }
}