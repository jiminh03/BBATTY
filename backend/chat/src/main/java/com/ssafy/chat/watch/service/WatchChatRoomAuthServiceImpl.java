package com.ssafy.chat.watch.service;

import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.common.dto.AuthResult;
import com.ssafy.chat.common.dto.SessionToken;
import com.ssafy.chat.common.dto.UserInfo;
import com.ssafy.chat.common.service.SessionTokenService;
import com.ssafy.chat.common.util.ChatRoomUtils;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.watch.dto.WatchChatJoinRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 직관 채팅방 인증 서비스 구현체
 * 완전 무명 채팅 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatchChatRoomAuthServiceImpl implements WatchChatRoomAuthService {

    private final ChatAuthRequestProducer chatAuthRequestProducer;
    private final ChatAuthResultService chatAuthResultService;
    private final ChatRoomUtils chatRoomUtils;
    private final SessionTokenService sessionTokenService;

    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, WatchChatJoinRequest request) {
        try {
            // 1. 채팅방 정보 생성 
            Map<String, Object> roomInfo = createRoomInfo(request);
            
            // 2. bbatty 서버에 인증 요청
            String requestId = chatAuthRequestProducer.sendWatchChatJoinRequest(
                    jwtToken, request.getGameId(), roomInfo);
            
            if (requestId == null) {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            // 3. 인증 결과 대기
            Map<String, Object> authResultMap = chatAuthResultService.waitForAuthResult(
                    requestId, (int) chatRoomUtils.getAuthTimeoutMs());
            
            if (authResultMap == null) {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            // 4. AuthResult 객체로 변환
            AuthResult authResult = mapToAuthResult(authResultMap);
            
            // 5. 직관 채팅용 UserInfo 생성 (profileImgUrl 제외)
            UserInfo userInfo = UserInfo.builder()
                    .userId(authResult.getUserInfo().getUserId())
                    .nickname(authResult.getUserInfo().getNickname())
                    .teamId(authResult.getUserInfo().getTeamId())
                    .teamName(authResult.getUserInfo().getTeamName())
                    .age(authResult.getUserInfo().getAge())
                    .gender(authResult.getUserInfo().getGender())
                    // profileImgUrl, winRate, isWinFairy 제외 (직관 채팅방에서 불필요)
                    .build();
            
            // 6. 세션 토큰 생성
            SessionToken sessionToken = sessionTokenService.createToken(
                    userInfo,
                    String.valueOf(request.getGameId()), // roomId로 gameId 사용
                    "WATCH",
                    request.getGameId()
            );
            
            // 7. 세션 정보 반환
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionToken", sessionToken.getToken());
            sessionData.put("userId", userInfo.getUserId());
            sessionData.put("nickname", userInfo.getNickname());
            sessionData.put("teamId", userInfo.getTeamId());
            sessionData.put("teamName", userInfo.getTeamName());
            sessionData.put("gameId", request.getGameId());
            sessionData.put("expiresIn", sessionToken.getExpiresIn() * 1000); // 초를 밀리초로 변환
            
            return sessionData;
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("직관 채팅방 세션 생성 실패 - gameId: {}", request.getGameId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
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
    
    
    @Override
    public Map<String, Object> getUserInfoByToken(String sessionToken) {
        try {
            var tokenInfo = sessionTokenService.validateToken(sessionToken);
            if (tokenInfo == null || !tokenInfo.isValid()) {
                return null;
            }
            
            // 직관 채팅방용 사용자 정보 반환
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", tokenInfo.getUserId());
            userInfo.put("nickname", tokenInfo.getNickname());
            userInfo.put("teamId", tokenInfo.getTeamId());
            userInfo.put("teamName", tokenInfo.getTeamName());
            userInfo.put("gameId", tokenInfo.getGameId());
            userInfo.put("chatType", tokenInfo.getRoomType());
            userInfo.put("age", tokenInfo.getAge());
            userInfo.put("gender", tokenInfo.getGender());
            // 직관 채팅방은 profileImgUrl 불필요
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("직관 채팅방 토큰 검증 실패 - sessionToken: {}", sessionToken, e);
            return null;
        }
    }
    
    @Override
    public void invalidateSession(String sessionToken) {
        sessionTokenService.invalidateToken(sessionToken);
    }
    
    /**
     * Map을 AuthResult로 변환
     */
    private AuthResult mapToAuthResult(Map<String, Object> authResultMap) {
        Boolean success = (Boolean) authResultMap.get("success");
        
        if (!Boolean.TRUE.equals(success)) {
            String errorMessage = (String) authResultMap.getOrDefault("errorMessage", "인증에 실패했습니다.");
            ErrorCode errorCode = mapErrorMessage(errorMessage);
            throw new ApiException(errorCode);
        }
        
        // 사용자 정보 파싱
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfoMap = (Map<String, Object>) authResultMap.get("userInfo");
        
        UserInfo userInfo = UserInfo.builder()
                .userId(((Number) userInfoMap.get("userId")).longValue())
                .nickname((String) userInfoMap.get("nickname"))
                .teamId(userInfoMap.get("teamId") != null ? ((Number) userInfoMap.get("teamId")).longValue() : null)
                .teamName((String) userInfoMap.get("teamName"))
                .age(userInfoMap.get("age") != null ? ((Number) userInfoMap.get("age")).intValue() : null)
                .gender((String) userInfoMap.get("gender"))
                // 직관 채팅방은 profileImgUrl, winRate, isWinFairy 불필요
                .build();
        
        return AuthResult.builder()
                .success(true)
                .userInfo(userInfo)
                .additionalInfo(authResultMap)
                .build();
    }
    
    /**
     * 에러 메시지를 ErrorCode로 매핑
     */
    private ErrorCode mapErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return ErrorCode.UNAUTHORIZED;
        }
        
        if (errorMessage.contains("경기 정보를 찾을 수 없어요")) {
            return ErrorCode.NOT_FOUND;
        } else if (errorMessage.contains("토큰이 만료되었어요")) {
            return ErrorCode.UNAUTHORIZED;
        } else if (errorMessage.contains("인증에 실패했어요")) {
            return ErrorCode.UNAUTHORIZED;
        }
        
        return ErrorCode.UNAUTHORIZED;
    }
}