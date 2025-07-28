package com.ssafy.bbatty.global.util;

import com.ssafy.bbatty.domain.auth.constants.AuthConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final RedisUtil redisUtil;
    private final SecretKey secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;
    private final String issuer;

    public JwtUtil(
            RedisUtil redisUtil,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer) {
        this.redisUtil = redisUtil;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidity = Duration.ofMinutes(30);  // 30분
        this.refreshTokenValidity = Duration.ofDays(14);    // 14일
        this.issuer = issuer;
    }

    public String generateAccessToken(Long userId) {
        return generateToken(userId, "access", accessTokenValidity);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, "refresh", refreshTokenValidity);
    }

    private String generateToken(Long userId, String type, Duration validity) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity.toMillis());

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setIssuer(issuer)
                .claim("type", type)
                .signWith(secretKey)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getTokenTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = AuthConstants.REDIS_BLACKLIST_PREFIX + token;
        return redisUtil.hasKey(blacklistKey);
    }

    // 토큰 검증 시 블랙리스트 확인 추가
    public boolean validateToken(String token) {
        try {
            if (isTokenBlacklisted(token)) {
                return false;
            }
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}