package com.ssafy.bbatty.global.security;

import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Role;
import com.ssafy.bbatty.global.exception.ApiException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 클래스
 * Access Token에는 사용자의 기본 정보(userId, age, gender, teamId)를 포함하여
 * 매번 DB 조회를 방지합니다.
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final String issuer;
    private final long accessTokenValidityInHours;
    private final long refreshTokenValidityInDays;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-validity-hours:1}") long accessTokenValidityInHours,
            @Value("${jwt.refresh-token-validity-days:14}") long refreshTokenValidityInDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.accessTokenValidityInHours = accessTokenValidityInHours;
        this.refreshTokenValidityInDays = refreshTokenValidityInDays;
    }

    /**
     * Access Token 생성
     * 사용자의 기본 정보를 클레임에 포함하여 DB 조회 최소화
     */
    public String createAccessToken(Long userId, int age, String gender, Long teamId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenValidityInHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(userId.toString())           // 사용자 ID
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("age", age)                 // 나이
                .claim("gender", gender)           // 성별
                .claim("teamId", teamId)           // 팀 정보
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     * 단순한 사용자 식별 정보만 포함
     */
    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenValidityInDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("tokenType", "refresh")
                .signWith(secretKey)
                .compact();
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SecurityException | MalformedJwtException | io.jsonwebtoken.security.SignatureException e) {
            log.warn("잘못된 JWT 서명입니다: {}", e.getMessage());
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        Claims  claims = getClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 토큰에서 사용자 나이 추출
     */
    public int getAge(String token) {
        Claims claims = getClaims(token);
        return claims.get("age", Integer.class);
    }

    /**
     * 토큰에서 사용자 성별 추출
     */
    public String getGender(String token) {
        Claims claims = getClaims(token);
        return claims.get("gender", String.class);
    }

    /**
     * 토큰에서 팀 ID 추출
     */
    public Long getTeamId(String token) {
        Claims claims = getClaims(token);
        return claims.get("teamId", Long.class);
    }

    /**
     * Access Token 검증
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = getClaims(token);

            // Refresh Token이 아닌지 확인
            if (claims.get("tokenType") != null) {
                return false;
            }
            return true;
        } catch (ApiException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaims(token);

            // Refresh Token 인지 확인
            return "refresh".equals(claims.get("tokenType"));
        } catch (ApiException e) {
            return false;
        }
    }

    /**
     * 토큰의 만료 시간 반환
     */
    public Date getExpiration(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration();
    }

    /**
     * Bearer 접두사 제거
     */
    public String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
