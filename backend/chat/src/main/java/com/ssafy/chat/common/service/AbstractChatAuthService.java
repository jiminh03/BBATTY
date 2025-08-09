package com.ssafy.chat.common.service;

import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.common.util.ChatRoomUtils;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.config.ChatProperties;
import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 채팅 인증 서비스 공통 추상 클래스
 * Match/Watch 채팅 인증의 공통 로직을 제공
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChatAuthService {

    protected final JwtProvider jwtProvider;
    protected final RedisUtil redisUtil;
    protected final ChatAuthRequestProducer chatAuthRequestProducer;
    protected final ChatAuthResultService chatAuthResultService;
    protected final ChatProperties chatProperties;
    protected final ChatRoomUtils chatRoomUtils;

    /**
     * 공통 인증 및 세션 생성 프로세스
     * 1. bbatty 서버 인증 요청
     * 2. 인증 결과 대기
     * 3. 세션 생성 및 Redis 저장
     */
    protected Map<String, Object> performAuthentication(String jwtToken, Object request, String requestId) {
        log.info("채팅 인증 요청 처리 시작 - requestId: {}", requestId);

        if (requestId == null) {
            throw new ApiException(ErrorCode.KAFKA_MESSAGE_SEND_FAILED);
        }

        // 인증 결과 대기
        Map<String, Object> authResult = chatAuthResultService.waitForAuthResult(
                requestId, chatRoomUtils.getAuthTimeoutMs());

        if (authResult == null) {
            log.error("bbatty 서버 인증 응답 타임아웃 - requestId: {}", requestId);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }

        // 인증 결과 검증
        validateAuthResult(authResult);

        // 인증 성공 시 세션 생성
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) authResult.get("userInfo");

        String sessionToken = generateSessionToken();
        Map<String, Object> sessionInfo = createSessionInfo(userInfo, request);
        
        // Redis에 세션 저장
        String sessionKey = getSessionKeyPrefix() + sessionToken;
        redisUtil.setValue(sessionKey, sessionInfo, getSessionExpireTime());

        log.info("채팅 세션 생성 완료 - userId: {}, sessionToken: {}", 
                userInfo.get("userId"), sessionToken);

        // 응답 생성
        return createAuthResponse(sessionToken, userInfo, request);
    }

    /**
     * 인증 결과 검증 및 에러 처리
     */
    private void validateAuthResult(Map<String, Object> authResult) {
        Boolean success = (Boolean) authResult.get("success");
        if (!success) {
            String errorMessage = (String) authResult.get("errorMessage");
            log.warn("bbatty 서버 인증 실패 - errorMessage: {}", errorMessage);
            
            ErrorCode errorCode = mapErrorMessage(errorMessage);
            throw new ApiException(errorCode);
        }
    }

    /**
     * 에러 메시지를 ErrorCode로 매핑
     */
    private ErrorCode mapErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return ErrorCode.UNAUTHORIZED;
        }

        if (errorMessage.contains("경기 정보를 찾을 수 없어요")) {
            return ErrorCode.GAME_NOT_FOUND;
        } else if (errorMessage.contains("이미 종료된 경기예요")) {
            return ErrorCode.GAME_FINISHED;
        } else if (errorMessage.contains("매칭 채팅방을 찾을 수 없어요")) {
            return ErrorCode.MATCH_CHAT_ROOM_NOT_FOUND;
        } else if (errorMessage.contains("매칭 채팅방이 비활성화")) {
            return ErrorCode.MATCH_CHAT_ROOM_CLOSED;
        } else if (errorMessage.contains("해당 팀에 대한 권한이 없어요")) {
            return ErrorCode.UNAUTHORIZED_TEAM_ACCESS;
        } else {
            return ErrorCode.UNAUTHORIZED;
        }
    }

    /**
     * 세션 토큰으로 사용자 정보 조회
     */
    public Map<String, Object> getUserInfoByToken(String sessionToken) {
        try {
            String sessionKey = getSessionKeyPrefix() + sessionToken;
            Object sessionData = redisUtil.getValue(sessionKey);
            
            if (sessionData == null) {
                throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) sessionData;
            
            log.debug("세션 토큰 검증 성공 - userId: {}", userInfo.get("userId"));
            return userInfo;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("세션 토큰 검증 실패 - token: {}", sessionToken, e);
            throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN);
        }
    }

    /**
     * 세션 무효화
     */
    public void invalidateSession(String sessionToken) {
        try {
            String sessionKey = getSessionKeyPrefix() + sessionToken;
            redisUtil.deleteKey(sessionKey);
            log.info("세션 무효화 완료 - sessionToken: {}", sessionToken);
        } catch (Exception e) {
            log.error("세션 무효화 실패 - sessionToken: {}", sessionToken, e);
        }
    }

    /**
     * 세션 토큰 생성
     */
    protected String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 추상 메서드들 - 구현체에서 오버라이드
    protected abstract String getSessionKeyPrefix();
    protected abstract Duration getSessionExpireTime();
    protected abstract Map<String, Object> createSessionInfo(Map<String, Object> userInfo, Object request);
    protected abstract Map<String, Object> createAuthResponse(String sessionToken, Map<String, Object> userInfo, Object request);
}