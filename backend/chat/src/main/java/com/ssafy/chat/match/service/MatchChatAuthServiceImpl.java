package com.ssafy.chat.match.service;

import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.match.dto.MatchChatJoinRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 매칭 채팅 인증 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatAuthServiceImpl implements MatchChatAuthService {

    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;

    private static final String SESSION_KEY_PREFIX = "match_chat_session:";
    private static final Duration SESSION_EXPIRE_TIME = Duration.ofHours(2);

    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, MatchChatJoinRequest request) {
        try {
            if (!jwtProvider.validateAccessToken(jwtToken)) {
                throw new SecurityException("유효하지 않은 JWT 토큰입니다.");
            }

            Long userId = jwtProvider.getUserId(jwtToken);
            String gender = jwtProvider.getGender(jwtToken);
            Integer age = jwtProvider.getAge(jwtToken);
            Long teamId = jwtProvider.getTeamId(jwtToken);

            log.info("JWT 토큰 파싱 성공 - userId: {}, gender: {}, age: {}, teamId: {}", 
                    userId, gender, age, teamId);

            validateMatchRoomAccess(request.getMatchId(), userId, gender, age, teamId);

            Map<String, Object> sessionInfo = createSessionInfo(userId, request, gender, age, teamId);

            String sessionToken = generateSessionToken();
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            
            redisUtil.setValue(sessionKey, sessionInfo, SESSION_EXPIRE_TIME);
            
            log.info("매칭 채팅 세션 생성 완료 - userId: {}, matchId: {}, sessionToken: {}", 
                    userId, request.getMatchId(), sessionToken);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionToken", sessionToken);
            response.put("userId", userId.toString());
            response.put("matchId", request.getMatchId());
            response.put("expiresIn", SESSION_EXPIRE_TIME.getSeconds());

            return response;

        } catch (SecurityException e) {
            log.error("JWT 토큰 검증 실패", e);
            throw e;
        } catch (Exception e) {
            log.error("매칭 채팅 세션 생성 실패", e);
            throw new RuntimeException("세션 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfoByToken(String sessionToken) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            Object sessionData = redisUtil.getValue(sessionKey);
            
            if (sessionData == null) {
                throw new SecurityException("유효하지 않은 세션 토큰입니다.");
            }

            Map<String, Object> userInfo = (Map<String, Object>) sessionData;
            
            log.debug("세션 토큰 검증 성공 - userId: {}, matchId: {}", 
                    userInfo.get("userId"), userInfo.get("matchId"));
            
            return userInfo;

        } catch (Exception e) {
            log.warn("세션 토큰 검증 실패 - token: {}", sessionToken, e);
            throw new SecurityException("세션 검증에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void invalidateSession(String sessionToken) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            redisUtil.deleteKey(sessionKey);
            log.info("세션 무효화 완료 - sessionToken: {}", sessionToken);
        } catch (Exception e) {
            log.error("세션 무효화 실패 - sessionToken: {}", sessionToken, e);
        }
    }

    private void validateMatchRoomAccess(String matchId, Long userId, String gender, Integer age, Long teamId) {
        if (matchId == null || matchId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 매칭 ID입니다.");
        }
        
        if (userId == null) {
            throw new SecurityException("사용자 정보가 유효하지 않습니다.");
        }
        
        if (age == null || age < 10 || age > 100) {
            throw new IllegalArgumentException("유효하지 않은 연령입니다.");
        }
        
        log.info("매칭방 입장 조건 검증 통과 - matchId: {}, userId: {}", matchId, userId);
    }

    private Map<String, Object> createSessionInfo(Long userId, MatchChatJoinRequest request, 
                                                String gender, Integer age, Long teamId) {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        sessionInfo.put("userId", userId.toString());
        sessionInfo.put("gender", gender);
        sessionInfo.put("age", age);
        sessionInfo.put("teamId", teamId);
        
        sessionInfo.put("matchId", request.getMatchId());
        sessionInfo.put("userName", request.getNickname());
        sessionInfo.put("nickname", request.getNickname());
        sessionInfo.put("winRate", request.getWinRate());
        sessionInfo.put("profileImgUrl", request.getProfileImgUrl());
        sessionInfo.put("isVictoryFairy", request.isVictoryFairy());
        
        sessionInfo.put("createdAt", System.currentTimeMillis());
        
        return sessionInfo;
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
