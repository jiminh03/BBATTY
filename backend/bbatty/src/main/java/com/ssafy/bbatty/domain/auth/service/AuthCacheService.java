package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.RedisKey;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;

import static com.ssafy.bbatty.global.constants.ErrorCode.SHA_256_NOT_FOUND;

/**
 * 인증 도메인 전용 Redis 서비스
 * - JWT 토큰 블랙리스트 관리
 * - 리프레시 토큰은 JWT 자체 검증으로 처리 (Stateless)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * JWT 토큰을 블랙리스트에 추가
     */
    public void blacklistToken(String token, Date expiration) {
        String key = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);
        long ttl = expiration.getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttl));
            log.debug("토큰 블랙리스트 추가: key={}", key);
        }
    }

    /**
     * 토큰 블랙리스트 확인
     */
    public boolean isTokenBlacklisted(String token) {
        String key = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);
        return redisTemplate.hasKey(key);
    }

    /**
     * 토큰 보안을 위한 SHA-256 해시 처리
     * - 충돌 가능성 최소화
     * - 보안 표준 준수
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            // Base64 인코딩
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(ErrorCode.SHA_256_NOT_FOUND);
        }
    }
}