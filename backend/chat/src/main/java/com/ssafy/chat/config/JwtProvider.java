package com.ssafy.chat.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 검증 및 사용자 정보 추출을 담당하는 클래스
 * (채팅 서비스용 - 토큰 생성 기능 제외)
 */
@Slf4j
@Component
public class JwtProvider {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
    }

    public DecodedJWT getClaims(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
            throw new SecurityException("유효하지 않은 JWT 토큰입니다.");
        }
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        DecodedJWT jwt = getClaims(token);
        return Long.valueOf(jwt.getSubject());
    }

    /**
     * 토큰에서 사용자 나이 추출
     */
    public Integer getAge(String token) {
        DecodedJWT jwt = getClaims(token);
        return jwt.getClaim("age").asInt();
    }

    /**
     * 토큰에서 사용자 성별 추출
     */
    public String getGender(String token) {
        DecodedJWT jwt = getClaims(token);
        return jwt.getClaim("gender").asString();
    }

    /**
     * 토큰에서 팀 ID 추출
     */
    public Long getTeamId(String token) {
        DecodedJWT jwt = getClaims(token);
        return jwt.getClaim("teamId").asLong();
    }

    /**
     * Access Token 검증
     */
    public boolean validateAccessToken(String token) {
        try {
            DecodedJWT jwt = getClaims(token);
            // Refresh Token이 아닌지 확인
            if (jwt.getClaim("tokenType").asString() != null) {
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