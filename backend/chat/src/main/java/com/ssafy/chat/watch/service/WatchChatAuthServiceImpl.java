package com.ssafy.chat.watch.service;

import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.common.service.AbstractChatAuthService;
import com.ssafy.chat.common.util.ChatRoomUtils;
import com.ssafy.chat.config.ChatProperties;
import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.watch.dto.WatchChatJoinRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 직관 채팅 인증 서비스 구현체
 * 완전 무명 채팅 시스템
 */
@Service
@Slf4j
public class WatchChatAuthServiceImpl extends AbstractChatAuthService implements WatchChatAuthService {

    private static final String SESSION_KEY_PREFIX = "watch_chat_session:";
    private static final Duration SESSION_EXPIRE_TIME = Duration.ofHours(3);

    public WatchChatAuthServiceImpl(JwtProvider jwtProvider, RedisUtil redisUtil,
                                   ChatAuthRequestProducer chatAuthRequestProducer,
                                   ChatAuthResultService chatAuthResultService,
                                   ChatProperties chatProperties, ChatRoomUtils chatRoomUtils) {
        super(jwtProvider, redisUtil, chatAuthRequestProducer, chatAuthResultService,
              chatProperties, chatRoomUtils);
    }

    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, WatchChatJoinRequest request) {
        // 채팅방 정보 생성
        Map<String, Object> roomInfo = createRoomInfo(request);
        
        // bbatty 서버에 인증 요청 전송
        String requestId = chatAuthRequestProducer.sendWatchChatJoinRequest(
                jwtToken, request.getGameId(), roomInfo);
        
        // 공통 인증 프로세스 수행
        return performAuthentication(jwtToken, request, requestId);
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

    // AbstractChatAuthService의 추상 메서드 구현
    @Override
    protected String getSessionKeyPrefix() {
        return SESSION_KEY_PREFIX;
    }

    @Override
    protected Duration getSessionExpireTime() {
        return SESSION_EXPIRE_TIME;
    }

    @Override
    protected Map<String, Object> createSessionInfo(Map<String, Object> userInfo, Object requestObj) {
        WatchChatJoinRequest request = (WatchChatJoinRequest) requestObj;
        return createSessionInfoFromAuth(userInfo, request);
    }

    @Override
    protected Map<String, Object> createAuthResponse(String sessionToken, Map<String, Object> userInfo, Object requestObj) {
        WatchChatJoinRequest request = (WatchChatJoinRequest) requestObj;
        
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

    // 레거시 메서드 - 더 이상 사용하지 않음
    @SuppressWarnings("unused")
    private Map<String, Object> createSessionInfo(Long teamId, WatchChatJoinRequest request) {
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("teamId", teamId);
        sessionInfo.put("gameId", request.getGameId());
        sessionInfo.put("isAttendanceVerified", request.isAttendanceVerified());
        sessionInfo.put("createdAt", System.currentTimeMillis());
        return sessionInfo;
    }
}