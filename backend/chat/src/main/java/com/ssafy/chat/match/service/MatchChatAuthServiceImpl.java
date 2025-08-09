package com.ssafy.chat.match.service;

import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.common.service.AbstractChatAuthService;
import com.ssafy.chat.common.util.ChatRoomUtils;
import com.ssafy.chat.config.ChatProperties;
import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.match.dto.MatchChatJoinRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 매칭 채팅 인증 서비스 구현체
 */
@Service
@Slf4j
public class MatchChatAuthServiceImpl extends AbstractChatAuthService implements MatchChatAuthService {

    private static final String SESSION_KEY_PREFIX = "match_chat_session:";
    private static final Duration SESSION_EXPIRE_TIME = Duration.ofHours(2);

    public MatchChatAuthServiceImpl(JwtProvider jwtProvider, RedisUtil redisUtil, 
                                  ChatAuthRequestProducer chatAuthRequestProducer,
                                  ChatAuthResultService chatAuthResultService,
                                  ChatProperties chatProperties, ChatRoomUtils chatRoomUtils) {
        super(jwtProvider, redisUtil, chatAuthRequestProducer, chatAuthResultService, 
              chatProperties, chatRoomUtils);
    }

    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, MatchChatJoinRequest request) {
        // 채팅방 정보 생성
        Map<String, Object> roomInfo = createRoomInfo(request);

        // bbatty 서버에 인증 요청 전송
        String requestId = chatAuthRequestProducer.sendMatchChatJoinRequest(
                jwtToken, request.getMatchId(), roomInfo, request.getNickname());

        // 공통 인증 프로세스 수행
        return performAuthentication(jwtToken, request, requestId);
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
        MatchChatJoinRequest request = (MatchChatJoinRequest) requestObj;
        return createSessionInfoFromAuth(userInfo, request);
    }

    @Override
    protected Map<String, Object> createAuthResponse(String sessionToken, Map<String, Object> userInfo, Object requestObj) {
        MatchChatJoinRequest request = (MatchChatJoinRequest) requestObj;
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionToken", sessionToken);
        response.put("userId", userInfo.get("userId"));
        response.put("nickname", request.getNickname()); // ✅ request에서 nickname 사용
        response.put("teamId", userInfo.get("teamId"));
        response.put("teamName", userInfo.get("teamName"));
        response.put("matchId", request.getMatchId());
        response.put("expiresIn", SESSION_EXPIRE_TIME.getSeconds());
        
        return response;
    }
    
    /**
     * 채팅방 정보 생성
     */
    private Map<String, Object> createRoomInfo(MatchChatJoinRequest request) {
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("matchId", request.getMatchId());
        roomInfo.put("nickname", request.getNickname());
        roomInfo.put("winRate", request.getWinRate());
        roomInfo.put("roomType", "MATCH");
        return roomInfo;
    }
    
    
    /**
     * 인증 결과로부터 세션 정보 생성
     */
    private Map<String, Object> createSessionInfoFromAuth(Map<String, Object> userInfo, MatchChatJoinRequest request) {
        // 디버깅: userInfo와 request 내용 확인
        log.info("DEBUG - userInfo 내용: {}", userInfo);
        log.info("DEBUG - request nickname: {}", request.getNickname());
        
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("userId", userInfo.get("userId"));
        sessionInfo.put("nickname", request.getNickname()); // ✅ request에서 nickname 사용
        sessionInfo.put("teamId", userInfo.get("teamId"));
        sessionInfo.put("teamName", userInfo.get("teamName"));
        sessionInfo.put("age", userInfo.get("age"));
        sessionInfo.put("gender", userInfo.get("gender"));
        sessionInfo.put("matchId", request.getMatchId());
        sessionInfo.put("chatType", "MATCH");
        sessionInfo.put("createdAt", System.currentTimeMillis());
        
        // 매칭 채팅 특정 정보
        sessionInfo.put("winRate", request.getWinRate());
        sessionInfo.put("profileImgUrl", request.getProfileImgUrl());
        sessionInfo.put("isWinFairy", request.isWinFairy());
        
        log.info("DEBUG - 최종 sessionInfo: {}", sessionInfo);
        return sessionInfo;
    }
    
}
