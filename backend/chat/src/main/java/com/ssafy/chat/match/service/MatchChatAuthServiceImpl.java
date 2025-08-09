package com.ssafy.chat.match.service;

import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
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
    private final ChatAuthRequestProducer chatAuthRequestProducer;
    private final ChatAuthResultService chatAuthResultService;

    private static final String SESSION_KEY_PREFIX = "match_chat_session:";
    private static final Duration SESSION_EXPIRE_TIME = Duration.ofHours(2);

    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, MatchChatJoinRequest request) {
        // 1. 채팅방 정보 생성
        Map<String, Object> roomInfo = createRoomInfo(request);

        // 2. bbatty 서버에 인증 요청 전송
        String requestId = chatAuthRequestProducer.sendMatchChatJoinRequest(
                jwtToken,
                request.getMatchId(),
                roomInfo,
                request.getNickname()
        );

        if (requestId == null) {
            throw new ApiException(ErrorCode.KAFKA_MESSAGE_SEND_FAILED);
        }

        log.info("bbatty 서버 인증 요청 완료: requestId={}, matchId={}", requestId, request.getMatchId());

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
            } else if (errorMessage != null && errorMessage.contains("매칭 채팅방을 찾을 수 없어요")) {
                throw new ApiException(ErrorCode.MATCH_CHAT_ROOM_NOT_FOUND);
            } else if (errorMessage != null && errorMessage.contains("매칭 채팅방이 비활성화")) {
                throw new ApiException(ErrorCode.MATCH_CHAT_ROOM_CLOSED);
            } else {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 4. 인증 성공 시 세션 생성
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) authResult.get("userInfo");

        log.info("userInfo 내용: {}", userInfo);  // 디버깅 로그 추가

        String sessionToken = generateSessionToken();
        String sessionKey = SESSION_KEY_PREFIX + sessionToken;

        Map<String, Object> sessionInfo = createSessionInfoFromAuth(userInfo, request);
        log.info("sessionInfo 생성 완료: {}", sessionInfo);  // 디버깅 로그 추가

        redisUtil.setValue(sessionKey, sessionInfo, SESSION_EXPIRE_TIME);
        log.info("Redis 저장 완료");  // 디버깅 로그 추가

        log.info("매칭 채팅 세션 생성 완료 - userId: {}, matchId: {}, sessionToken: {}",
                userInfo.get("userId"), request.getMatchId(), sessionToken);

        // 5. 응답 생성
        Map<String, Object> response = new HashMap<>();
        response.put("sessionToken", sessionToken);
        response.put("userId", userInfo.get("userId"));
        response.put("nickname", userInfo.get("nickname"));
        response.put("teamId", userInfo.get("teamId"));
        response.put("teamName", userInfo.get("teamName"));
        response.put("matchId", request.getMatchId());
        response.put("expiresIn", SESSION_EXPIRE_TIME.getSeconds());

        log.info("응답 생성 완료: {}", response);  // 디버깅 로그 추가

        return response;
    }

    @Override
    public Map<String, Object> getUserInfoByToken(String sessionToken) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            Object sessionData = redisUtil.getValue(sessionKey);
            
            if (sessionData == null) {
                throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN);
            }

            Map<String, Object> userInfo = (Map<String, Object>) sessionData;
            
            log.debug("세션 토큰 검증 성공 - userId: {}, matchId: {}", 
                    userInfo.get("userId"), userInfo.get("matchId"));
            
            return userInfo;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("세션 토큰 검증 실패 - token: {}", sessionToken, e);
            throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN);
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
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("userId", userInfo.get("userId"));
        sessionInfo.put("nickname", userInfo.get("nickname"));
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
        
        return sessionInfo;
    }
    
}
