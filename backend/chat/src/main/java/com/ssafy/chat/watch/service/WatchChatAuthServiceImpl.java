package com.ssafy.chat.watch.service;

import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.global.constants.ErrorCode;
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
    private final ChatAuthRequestProducer chatAuthRequestProducer;
    private final ChatAuthResultService chatAuthResultService;

    private static final String SESSION_KEY_PREFIX = "watch_chat_session:";
    private static final Duration SESSION_EXPIRE_TIME = Duration.ofHours(3);

    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, WatchChatJoinRequest request) {
        // 1. 채팅방 정보 생성
        Map<String, Object> roomInfo = createRoomInfo(request);
        
        // 2. bbatty 서버에 인증 요청 전송
        String requestId = chatAuthRequestProducer.sendWatchChatJoinRequest(
            jwtToken, 
            request.getGameId(), 
            roomInfo
        );
        
        if (requestId == null) {
            throw new ApiException(ErrorCode.KAFKA_MESSAGE_SEND_FAILED);
        }
        
        log.info("bbatty 서버 인증 요청 완료: requestId={}, gameId={}", requestId, request.getGameId());
        
        // 3. 인증 결과 대기 및 폴링 (최대 10초)
        Map<String, Object> authResult = chatAuthResultService.waitForAuthResult(requestId, 10000);
        
        if (authResult == null) {
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
        
        Boolean success = (Boolean) authResult.get("success");
        if (!success) {
            String errorMessage = (String) authResult.get("errorMessage");
            
            // bbatty 서버에서 온 에러 메시지에 따라 적절한 ErrorCode 선택
            if (errorMessage != null && errorMessage.contains("경기 정보를 찾을 수 없어요")) {
                throw new ApiException(ErrorCode.GAME_NOT_FOUND);
            } else if (errorMessage != null && errorMessage.contains("이미 종료된 경기예요")) {
                throw new ApiException(ErrorCode.GAME_FINISHED);
            } else if (errorMessage != null && errorMessage.contains("해당 팀에 대한 권한이 없어요")) {
                throw new ApiException(ErrorCode.UNAUTHORIZED_TEAM_ACCESS);
            } else {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
        }
        
        // 4. 인증 성공 시 세션 생성
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) authResult.get("userInfo");
        
        String sessionToken = generateSessionToken();
        String sessionKey = SESSION_KEY_PREFIX + sessionToken;
        
        Map<String, Object> sessionInfo = createSessionInfoFromAuth(userInfo, request);
        redisUtil.setValue(sessionKey, sessionInfo, SESSION_EXPIRE_TIME);
        
        log.info("직관 채팅 세션 생성 완료 - userId: {}, gameId: {}, sessionToken: {}", 
                userInfo.get("userId"), request.getGameId(), sessionToken);

        // 5. 응답 생성
        Map<String, Object> response = new HashMap<>();
        response.put("sessionToken", sessionToken);
        response.put("userId", ((Number) userInfo.get("userId")).longValue());
        response.put("nickname", userInfo.get("nickname"));
        response.put("teamId", ((Number) userInfo.get("teamId")).longValue());
        response.put("teamName", userInfo.get("teamName"));
        response.put("gameId", request.getGameId());
        response.put("expiresIn", SESSION_EXPIRE_TIME.getSeconds());

        return response;
    }
    
    /**
     * 채팅방 정보 생성
     */
    private Map<String, Object> createRoomInfo(WatchChatJoinRequest request) {
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("gameId", request.getGameId());
        roomInfo.put("teamId", request.getTeamId());
        roomInfo.put("roomType", "WATCH");
        return roomInfo;
    }
    
    
    /**
     * 인증 결과로부터 세션 정보 생성
     */
    private Map<String, Object> createSessionInfoFromAuth(Map<String, Object> userInfo, WatchChatJoinRequest request) {
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("userId", ((Number) userInfo.get("userId")).longValue());
        sessionInfo.put("nickname", userInfo.get("nickname"));
        sessionInfo.put("teamId", ((Number) userInfo.get("teamId")).longValue());
        sessionInfo.put("teamName", userInfo.get("teamName"));
        sessionInfo.put("gameId", request.getGameId());
        sessionInfo.put("chatType", "WATCH");
        sessionInfo.put("createdAt", System.currentTimeMillis());
        return sessionInfo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfoByToken(String sessionToken) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            Object sessionData = redisUtil.getValue(sessionKey);
            
            if (sessionData == null) {
                throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN);
            }

            Map<String, Object> userInfo = (Map<String, Object>) sessionData;
            
            log.debug("세션 토큰 검증 성공 - teamId: {}, gameId: {}", 
                    userInfo.get("teamId"), userInfo.get("gameId"));
            
            return userInfo;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("세션 토큰 검증 실패 - token: {}", sessionToken, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
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
    // private void validateWatchRoomAccess(...) { ... }

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