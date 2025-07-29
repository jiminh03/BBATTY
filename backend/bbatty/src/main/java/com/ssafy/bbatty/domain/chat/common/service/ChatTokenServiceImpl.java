package com.ssafy.bbatty.domain.chat.common.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ssafy.bbatty.domain.chat.common.dto.ChatTokenClaims;
import com.ssafy.bbatty.domain.chat.common.enums.ChatType;
import com.ssafy.bbatty.global.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * 채팅 토큰 생성 및 검증 서비스 구현체
 * 경기 채팅과 매칭 채팅 모두에서 사용되는 공통 토큰 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatTokenServiceImpl implements ChatTokenService {

    private final JwtProperties jwtProperties;

    @Override
    public String generateChatToken(String userId, ChatType chatType, Map<String, Object> claims) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtProperties.getSecret());
            
            var jwtBuilder = JWT.create()
                    .withSubject(userId)
                    .withClaim("chatType", chatType.name())
                    .withClaim("issuedAt", System.currentTimeMillis())
                    .withExpiresAt(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES))); // 5분 유효
            
            // 추가 클레임 정보 설정
            if (claims != null) {
                claims.forEach(jwtBuilder::withClaim);
            }
            
            String token = jwtBuilder.sign(algorithm);
            
            log.debug("채팅 토큰 생성 완료 - userId: {}, chatType: {}", userId, chatType);
            return token;
            
        } catch (Exception e) {
            log.error("채팅 토큰 생성 실패 - userId: {}, chatType: {}", userId, chatType, e);
            throw new RuntimeException("채팅 토큰 생성에 실패했습니다.", e);
        }
    }

    @Override
    public ChatTokenClaims validateChatToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtProperties.getSecret());
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            
            String userId = jwt.getSubject();
            String chatTypeStr = jwt.getClaim("chatType").asString();
            ChatType chatType = ChatType.valueOf(chatTypeStr);
            
            ChatTokenClaims claims = ChatTokenClaims.builder()
                    .userId(userId)
                    .chatType(chatType)
                    .issuedAt(jwt.getClaim("issuedAt").asLong())
                    .expiresAt(jwt.getExpiresAt().getTime())
                    .build();
            
            // 채팅 타입별 추가 클레임 추출
            switch (chatType) {
                case GAME:
                    claims.setTeamId(jwt.getClaim("teamId").asString());
                    claims.setGameId(jwt.getClaim("gameId").asLong());
                    break;
                case MATCH:
                    claims.setRoomId(jwt.getClaim("roomId").asString());
                    claims.setMatchId(jwt.getClaim("matchId").asString());
                    break;
            }
            
            log.debug("채팅 토큰 검증 성공 - userId: {}, chatType: {}", userId, chatType);
            return claims;
            
        } catch (JWTVerificationException e) {
            log.warn("채팅 토큰 검증 실패 - token: {}", token.substring(0, Math.min(token.length(), 20)) + "...", e);
            throw new RuntimeException("유효하지 않은 채팅 토큰입니다.", e);
        } catch (Exception e) {
            log.error("채팅 토큰 처리 중 오류 발생", e);
            throw new RuntimeException("채팅 토큰 처리에 실패했습니다.", e);
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            ChatTokenClaims claims = validateChatToken(token);
            return claims.getExpiresAt() < System.currentTimeMillis();
        } catch (Exception e) {
            return true; // 검증 실패 시 만료된 것으로 처리
        }
    }

    @Override
    public String extractUserIdWithoutValidation(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getSubject();
        } catch (Exception e) {
            log.warn("토큰에서 사용자 ID 추출 실패", e);
            return null;
        }
    }
}