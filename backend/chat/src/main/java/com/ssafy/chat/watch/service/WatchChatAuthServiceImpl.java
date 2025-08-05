package com.ssafy.chat.watch.service;

import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.watch.dto.WatchChatJoinRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 직관 채팅 인증 서비스 구현체
 * 완전 무명 채팅 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatchChatAuthServiceImpl implements WatchChatAuthService {

    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;

    private static final String SESSION_KEY_PREFIX = "watch_chat_session:";
    private static final Duration SESSION_EXPIRE_TIME = Duration.ofHours(3);

    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, WatchChatJoinRequest request) {
        try {
            if (!jwtProvider.validateAccessToken(jwtToken)) {
                throw new SecurityException("유효하지 않은 JWT 토큰입니다.");
            }

            Long teamId = jwtProvider.getTeamId(jwtToken);
            
            log.info("JWT 토큰 파싱 성공 - teamId: {}", teamId);

            validateWatchRoomAccess(request.getGameId(), teamId, request.isAttendanceVerified());

            Map<String, Object> sessionInfo = createSessionInfo(teamId, request);

            String sessionToken = generateSessionToken();
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            
            redisUtil.setValue(sessionKey, sessionInfo, SESSION_EXPIRE_TIME);
            
            log.info("직관 채팅 세션 생성 완료 - teamId: {}, gameId: {}, sessionToken: {}", 
                    teamId, request.getGameId(), sessionToken);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionToken", sessionToken);
            response.put("teamId", teamId.toString());
            response.put("gameId", request.getGameId());
            response.put("expiresIn", SESSION_EXPIRE_TIME.getSeconds());

            return response;

        } catch (SecurityException e) {
            log.error("JWT 토큰 검증 실패", e);
            throw e;
        } catch (Exception e) {
            log.error("직관 채팅 세션 생성 실패", e);
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
            
            log.debug("세션 토큰 검증 성공 - teamId: {}, gameId: {}", 
                    userInfo.get("teamId"), userInfo.get("gameId"));
            
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

    private void validateWatchRoomAccess(String gameId, Long teamId, boolean isAttendanceVerified) {
        if (gameId == null || gameId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 게임 ID입니다.");
        }
        
        if (teamId == null) {
            throw new SecurityException("팀 정보가 유효하지 않습니다.");
        }
        
        if (!isAttendanceVerified) {
            throw new SecurityException("직관 인증이 필요합니다.");
        }
        
        log.info("직관방 입장 조건 검증 통과 - gameId: {}, teamId: {}", gameId, teamId);
    }

    private Map<String, Object> createSessionInfo(Long teamId, WatchChatJoinRequest request) {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        sessionInfo.put("teamId", teamId);
        sessionInfo.put("gameId", request.getGameId());
        sessionInfo.put("isAttendanceVerified", request.isAttendanceVerified());
        sessionInfo.put("createdAt", System.currentTimeMillis());
        
        return sessionInfo;
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}