package com.ssafy.bbatty.domain.auth.service;

import java.util.Date;

/**
 * 인증 도메인 전용 Redis 서비스 인터페이스
 */
public interface AuthCacheService {

    /**
     * JWT 토큰을 블랙리스트에 추가
     */
    void blacklistToken(String token, Date expiration);

    /**
     * 토큰 블랙리스트 확인
     */
    boolean isTokenBlacklisted(String token);

    /**
     * Redis 연결 상태를 확인하는 헬스체크 메서드
     */
    void checkRedisConnection();
}