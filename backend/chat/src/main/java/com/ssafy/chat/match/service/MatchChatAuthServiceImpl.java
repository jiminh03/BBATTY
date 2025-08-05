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
            // TODO: 나중에 Kafka로 bbatty 서버에 인증 요청
            // 현재는 모든 요청 허용하고 더미 데이터로 세션 생성
            
            // 닉네임과 matchId 기반으로 고유한 더미 데이터 생성
            Long userId = (long) Math.abs((request.getNickname() + request.getMatchId()).hashCode() % 10000 + 1);
            String gender = userId % 2 == 0 ? "M" : "F"; // 짝수면 남성, 홀수면 여성
            Integer age = (int) (userId % 30 + 20); // 20-49세
            Long teamId = (long) (userId % 2 + 1); // 1팀 또는 2팀

            log.info("매칭 채팅 요청 허용 - 더미 데이터 사용 - userId: {}, gender: {}, age: {}, teamId: {}", 
                    userId, gender, age, teamId);

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

    // 더 이상 사용하지 않음 - 모든 요청 허용
    // private void validateMatchRoomAccess(...) { ... }

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
        sessionInfo.put("isWinFairy", request.isWinFairy());
        
        sessionInfo.put("createdAt", System.currentTimeMillis());
        
        return sessionInfo;
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
