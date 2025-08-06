package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.RedisKey;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;

import static com.ssafy.bbatty.global.constants.ErrorCode.*;

/**
 * 인증 도메인 전용 Redis 서비스
 * - JWT 토큰 블랙리스트 관리
 * - 리프레시 토큰은 JWT 자체 검증으로 처리 (Stateless)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCacheServiceImpl implements AuthCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * JWT 토큰을 블랙리스트에 추가
     * Redis 연결 실패 시 로그만 남기고 계속 진행 (Graceful Degradation)
     */
    public void blacklistToken(String token, Date expiration) {
        try {
            String key = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttl));
                log.debug("토큰 블랙리스트 추가: key={}", key);
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 연결 실패로 토큰 블랙리스트 추가 실패: {}", e.getMessage());
            // Graceful Degradation: JWT 자체 만료 시간으로 보안 유지
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 중 예상치 못한 오류: {}", e.getMessage());
            // 예상치 못한 오류의 경우 계속 진행하되 로그 남김
        }
    }

    /**
     * 토큰 블랙리스트 확인
     * Redis 연결 실패 시 false 반환 (보수적 접근 - 토큰을 유효한 것으로 처리)
     *
     * @param token 확인할 JWT 토큰
     * @return 블랙리스트에 있으면 true, 없거나 Redis 오류 시 false
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = RedisKey.AUTH_TOKEN_BLACKLIST + hashToken(token);
            return redisTemplate.hasKey(key);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 연결 실패로 토큰 블랙리스트 확인 실패: {}", e.getMessage());
            // Graceful Degradation: 보수적 접근으로 false 반환
            return false;
        } catch (Exception e) {
            log.error("토큰 블랙리스트 확인 중 예상치 못한 오류: {}", e.getMessage());
            // 예상치 못한 오류의 경우 보수적 접근으로 false 반환
            return false;
        }
    }

    /**
     * Redis 연결 상태를 확인하는 헬스체크 메서드
     * 운영 환경에서 모니터링에 활용 가능
     *
     * @throws ApiException Redis 연결 실패 시 표준 에러 코드로 예외 발생
     */
    public void checkRedisConnection() {
        try {
            redisTemplate.hasKey("health-check");
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 헬스체크 실패 - 연결 불가: {}", e.getMessage());
            throw new ApiException(REDIS_CONNECTION_FAILED);
        } catch (Exception e) {
            log.error("Redis 헬스체크 실패 - 예상치 못한 오류: {}", e.getMessage());
            throw new ApiException(REDIS_OPERATION_FAILED);
        }
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
            log.error("SHA-256 알고리즘을 찾을 수 없음: {}", e.getMessage());
            throw new ApiException(SHA_256_NOT_FOUND);
        }
    }
}