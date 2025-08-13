package com.ssafy.chat.match.controller;

import com.ssafy.chat.common.util.AuthenticationUtil;
import com.ssafy.chat.common.util.ChatRoomUtils;
import com.ssafy.chat.config.ChatProperties;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.global.response.ApiResponse;
import com.ssafy.chat.match.dto.MatchChatJoinRequest;
import com.ssafy.chat.match.dto.MatchChatJoinResponse;
import com.ssafy.chat.match.service.MatchChatRoomAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 매칭 채팅 인증 컨트롤러
 * JWT 토큰과 클라이언트 정보를 기반으로 매칭 채팅방 입장 검증
 */
@RestController
@RequestMapping("/api/match-chat")
@RequiredArgsConstructor
@Slf4j
public class MatchChatAuthController {

    private final MatchChatRoomAuthService matchChatRoomAuthService;
    private final ChatProperties chatProperties;
    private final ChatRoomUtils chatRoomUtils;
    private final AuthenticationUtil authenticationUtil;

    /**
     * 매칭 채팅방 입장 요청
     * JWT 토큰 + 클라이언트 정보로 입장 조건 검증 후 세션 토큰 발급
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<MatchChatJoinResponse>> joinMatchChat(
            @RequestHeader(value = "Authorization", required = true) String authHeader,
            @Valid @RequestBody MatchChatJoinRequest request) {
        
        log.info("매칭 채팅방 입장 요청 - matchId: {}, nickname: {}, winRate: {}%", 
                request.getMatchId(), request.getNickname(), request.getWinRate());
        
        // JWT 토큰 추출
        String jwtToken = authenticationUtil.extractJwtToken(authHeader);
        
        try {
            // 입장 검증 및 세션 토큰 발급
            Map<String, Object> sessionData = matchChatRoomAuthService.validateAndCreateSession(jwtToken, request);
            
            // WebSocket 접속 링크 생성 - React Native용 순수 WebSocket
            String websocketUrl = String.format("%s/ws/match-chat?sessionToken=%s&matchId=%s",
                    chatProperties.getWebsocket().getBaseUrl(), 
                    sessionData.get("sessionToken"), request.getMatchId());

            // 응답 DTO 생성
            MatchChatJoinResponse response = MatchChatJoinResponse.builder()
                    .sessionToken((String) sessionData.get("sessionToken"))
                    .userId(((Number) sessionData.get("userId")).longValue())
                    .matchId(String.valueOf(request.getMatchId()))
                    .expiresIn((Long) sessionData.get("expiresIn"))
                    .websocketUrl(websocketUrl)
                    .nickname((String) sessionData.get("nickname"))
                    .teamId(sessionData.get("teamId") != null ? ((Number) sessionData.get("teamId")).longValue() : null)
                    .teamName((String) sessionData.get("teamName"))
                    .build();
            
            log.info("매칭 채팅방 입장 승인 - matchId: {}, userId: {}", 
                    request.getMatchId(), response.getUserId());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            log.warn("매칭 채팅방 입장 거부 - matchId: {}, reason: {}", 
                    request.getMatchId(), e.getMessage());
            throw new ApiException(ErrorCode.BAD_REQUEST);
            
        } catch (SecurityException e) {
            log.warn("매칭 채팅방 입장 인증 실패 - matchId: {}, reason: {}", 
                    request.getMatchId(), e.getMessage());
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 세션 토큰 검증 (WebSocket HandshakeInterceptor에서 사용)
     */
    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(@RequestParam String token) {
        try {
            Map<String, Object> userInfo = matchChatRoomAuthService.getUserInfoByToken(token);
            return ResponseEntity.ok(ApiResponse.success(userInfo));
        } catch (Exception e) {
            log.warn("세션 토큰 검증 실패 - token: {}", token, e);
            throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN);
        }
    }
}