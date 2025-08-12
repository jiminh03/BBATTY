package com.ssafy.chat.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.SessionToken;
import com.ssafy.chat.common.dto.SessionTokenInfo;
import com.ssafy.chat.common.dto.UserInfo;
import com.ssafy.chat.config.ChatConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 세션 토큰 관리 서비스
 * REST API 인증 후 WebSocket 연결용 세션 토큰을 생성하고 검증
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionTokenService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatConfiguration chatConfiguration;
    
    private static final String SESSION_TOKEN_KEY = "session:token:";
    private static final Duration DEFAULT_TOKEN_TTL = Duration.ofMinutes(30);
    
    /**
     * 세션 토큰 생성
     * @param userInfo 인증된 사용자 정보
     * @param roomId 채팅방 ID
     * @param roomType 채팅방 타입 (MATCH, WATCH)
     * @param gameId 게임 ID
     * @return 생성된 세션 토큰
     */
    public SessionToken createToken(UserInfo userInfo, String roomId, String roomType, Long gameId) {
        try {
            String token = generateSecureToken();
            Duration tokenTtl = getTokenTtl();
            
            SessionTokenInfo tokenInfo = SessionTokenInfo.builder()
                .token(token)
                .userId(userInfo.getUserId())
                .nickname(userInfo.getNickname())
                .teamId(userInfo.getTeamId())
                .teamName(userInfo.getTeamName())
                .roomId(roomId)
                .roomType(roomType)
                .gameId(gameId)
                .age(userInfo.getAge())
                .gender(userInfo.getGender())
                .winRate(userInfo.getWinRate())
                .isWinFairy(userInfo.getIsWinFairy())
                .profileImgUrl(userInfo.getProfileImgUrl())
                .issuedAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + tokenTtl.toMillis())
                .build();
            
            // 디버깅: 토큰에 저장된 profileImgUrl 확인
            log.info("SessionTokenInfo 생성 - profileImgUrl: {}", tokenInfo.getProfileImgUrl());
            
            // Redis에 토큰 정보 저장
            String tokenKey = SESSION_TOKEN_KEY + token;
            String tokenJson = objectMapper.writeValueAsString(tokenInfo);
            
            log.debug("Redis에 토큰 저장 시도 - key: {}, tokenTtl: {}", tokenKey, tokenTtl);
            redisTemplate.opsForValue().set(tokenKey, tokenJson, tokenTtl);
            
            // 저장 확인
            String savedToken = redisTemplate.opsForValue().get(tokenKey);
            if (savedToken != null) {
                log.info("세션 토큰 생성 및 저장 완료 - userId: {}, roomId: {}, token: {}...", 
                        userInfo.getUserId(), roomId, token.substring(0, 8));
            } else {
                log.error("세션 토큰 Redis 저장 실패 - userId: {}, roomId: {}, token: {}...", 
                        userInfo.getUserId(), roomId, token.substring(0, 8));
                throw new RuntimeException("세션 토큰 Redis 저장 실패");
            }
            
            return SessionToken.builder()
                .token(token)
                .expiresIn(tokenTtl.getSeconds())
                .issuedAt(tokenInfo.getIssuedAt())
                .tokenType("Bearer")
                .build();
                
        } catch (Exception e) {
            log.error("세션 토큰 생성 실패 - userId: {}, roomId: {}", 
                    userInfo.getUserId(), roomId, e);
            throw new RuntimeException("세션 토큰 생성 실패", e);
        }
    }
    
    /**
     * 세션 토큰 검증
     * @param token 검증할 토큰
     * @return 토큰 정보 (유효하지 않으면 null)
     */
    public SessionTokenInfo validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("빈 토큰 검증 시도");
            return null;
        }
        
        try {
            String tokenKey = SESSION_TOKEN_KEY + token;
            log.debug("토큰 검증 시도 - key: {}", tokenKey);
            String tokenJson = redisTemplate.opsForValue().get(tokenKey);
            
            if (tokenJson == null) {
                log.warn("존재하지 않는 토큰 - token: {}..., key: {}", 
                        token.substring(0, Math.min(8, token.length())), tokenKey);
                return null;
            }
            
            log.debug("토큰 정보 조회 성공 - token: {}..., jsonLength: {}", 
                    token.substring(0, Math.min(8, token.length())), tokenJson.length());
            
            SessionTokenInfo tokenInfo = objectMapper.readValue(tokenJson, SessionTokenInfo.class);
            
            if (tokenInfo.isExpired()) {
                log.info("만료된 토큰 - userId: {}, token: {}...", 
                        tokenInfo.getUserId(), token.substring(0, 8));
                
                // 만료된 토큰 삭제
                redisTemplate.delete(tokenKey);
                return null;
            }
            
            if (!tokenInfo.isValid()) {
                log.warn("유효하지 않은 토큰 정보 - token: {}...", token.substring(0, 8));
                return null;
            }
            
            log.debug("토큰 검증 성공 - userId: {}, roomId: {}", 
                    tokenInfo.getUserId(), tokenInfo.getRoomId());
            
            return tokenInfo;
            
        } catch (Exception e) {
            log.error("세션 토큰 검증 실패 - token: {}...", 
                    token.substring(0, Math.min(8, token.length())), e);
            return null;
        }
    }
    
    /**
     * 세션 토큰 무효화 (로그아웃 등)
     * @param token 무효화할 토큰
     * @return 성공 여부
     */
    public boolean invalidateToken(String token) {
        try {
            String tokenKey = SESSION_TOKEN_KEY + token;
            Boolean deleted = redisTemplate.delete(tokenKey);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.info("세션 토큰 무효화 완료 - token: {}...", token.substring(0, 8));
                return true;
            } else {
                log.debug("무효화할 토큰이 존재하지 않음 - token: {}...", token.substring(0, 8));
                return false;
            }
            
        } catch (Exception e) {
            log.error("세션 토큰 무효화 실패 - token: {}...", token.substring(0, 8), e);
            return false;
        }
    }
    
    /**
     * 사용자의 모든 세션 토큰 무효화
     * @param userId 사용자 ID
     * @return 무효화된 토큰 수
     */
    public int invalidateUserTokens(Long userId) {
        try {
            // 모든 세션 토큰을 스캔해서 해당 사용자의 토큰만 삭제
            // 실제 운영환경에서는 사용자별 토큰 인덱스를 별도로 관리하는 것이 효율적
            var keys = redisTemplate.keys(SESSION_TOKEN_KEY + "*");
            int deletedCount = 0;
            
            if (keys != null) {
                for (String key : keys) {
                    try {
                        String tokenJson = redisTemplate.opsForValue().get(key);
                        if (tokenJson != null) {
                            SessionTokenInfo tokenInfo = objectMapper.readValue(tokenJson, SessionTokenInfo.class);
                            if (userId.equals(tokenInfo.getUserId())) {
                                redisTemplate.delete(key);
                                deletedCount++;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("사용자 토큰 스캔 중 오류 - key: {}", key, e);
                    }
                }
            }
            
            log.info("사용자 세션 토큰 일괄 무효화 완료 - userId: {}, count: {}", userId, deletedCount);
            return deletedCount;
            
        } catch (Exception e) {
            log.error("사용자 세션 토큰 일괄 무효화 실패 - userId: {}", userId, e);
            return 0;
        }
    }
    
    /**
     * 토큰 갱신 (기존 토큰을 새로운 토큰으로 교체)
     * @param oldToken 기존 토큰
     * @return 새로운 세션 토큰 (실패시 null)
     */
    public SessionToken refreshToken(String oldToken) {
        SessionTokenInfo oldTokenInfo = validateToken(oldToken);
        if (oldTokenInfo == null) {
            return null;
        }
        
        // 기존 토큰 무효화
        invalidateToken(oldToken);
        
        // 새로운 토큰 생성
        UserInfo userInfo = UserInfo.builder()
            .userId(oldTokenInfo.getUserId())
            .nickname(oldTokenInfo.getNickname())
            .teamId(oldTokenInfo.getTeamId())
            .teamName(oldTokenInfo.getTeamName())
            .age(oldTokenInfo.getAge())
            .gender(oldTokenInfo.getGender())
            .winRate(oldTokenInfo.getWinRate())
            .isWinFairy(oldTokenInfo.getIsWinFairy())
            .profileImgUrl(oldTokenInfo.getProfileImgUrl())
            .build();
            
        return createToken(userInfo, oldTokenInfo.getRoomId(), 
                          oldTokenInfo.getRoomType(), oldTokenInfo.getGameId());
    }
    
    /**
     * 보안 토큰 생성
     * @return 32자리 무작위 토큰
     */
    private String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               Long.toHexString(System.currentTimeMillis()) + 
               Long.toHexString(ThreadLocalRandom.current().nextLong());
    }
    
    /**
     * 토큰 TTL 조회
     * @return 설정된 TTL (설정이 없으면 기본값 30분)
     */
    private Duration getTokenTtl() {
        try {
            Duration configuredTtl = chatConfiguration.getSessionTokenTtl();
            return configuredTtl != null ? configuredTtl : DEFAULT_TOKEN_TTL;
        } catch (Exception e) {
            log.debug("설정에서 토큰 TTL 조회 실패, 기본값 사용", e);
            return DEFAULT_TOKEN_TTL;
        }
    }
}