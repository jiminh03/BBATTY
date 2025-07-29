package com.ssafy.bbatty.global.security;

import com.ssafy.bbatty.global.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long userId) {
        return createToken(userId, "access", jwtProperties.getAccessTokenValidity());
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long userId) {
        return createToken(userId, "refresh", jwtProperties.getRefreshTokenValidity());
    }

    /**
     * 토큰 생성 공통 로직
     */
    private String createToken(Long userId, String type, Duration validity) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity.toMillis());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .issuer(jwtProperties.getIssuer())
                .claim("type", type)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 타입 확인
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    /**
     * 토큰 남은 유효시간 반환
     */
    public Duration getTokenRemainingValidity(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        long remainingMillis = Math.max(0, expiration.getTime() - now.getTime());
        return Duration.ofMillis(remainingMillis);
    }

    /**
     * Access Token 만료시간 (초) 반환
     */
    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenValidity().getSeconds();
    }

    /**
     * 토큰 파싱
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰 타입 추출
     */
    private String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }
}