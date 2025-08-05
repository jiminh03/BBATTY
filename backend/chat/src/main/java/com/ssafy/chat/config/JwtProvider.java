package com.ssafy.chat.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 검증 및 사용자 정보 추출을 담당하는 클래스
 * (채팅 서비스용 - 토큰 생성 기능 제외)
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final String issuer;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
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
            throw new SecurityException("유효하지 않은 JWT 토큰입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
            throw new SecurityException("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
            throw new SecurityException("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
            throw new SecurityException("잘못된 JWT 토큰입니다.");
        }
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
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
        } catch (Exception e) {
            return false;
        }
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