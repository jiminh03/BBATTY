package com.ssafy.chat.match.controller;

import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.global.response.ApiResponse;
import com.ssafy.chat.match.dto.MatchChatJoinRequest;
import com.ssafy.chat.match.dto.MatchChatJoinResponse;
import com.ssafy.chat.match.service.MatchChatAuthService;
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

    private final MatchChatAuthService matchChatAuthService;

    /**
     * 매칭 채팅방 입장 요청
     * JWT 토큰 + 클라이언트 정보로 입장 조건 검증 후 세션 토큰 발급
     */
    @PostMapping("/auth/join")
    public ResponseEntity<ApiResponse<MatchChatJoinResponse>> joinMatchChat(
            @RequestHeader(value = "Authorization", required = true) String authHeader,
            @Valid @RequestBody MatchChatJoinRequest request) {
        
        log.info("매칭 채팅방 입장 요청 - matchId: {}, nickname: {}, winRate: {}%", 
                request.getMatchId(), request.getNickname(), request.getWinRate());
        
        // JWT 토큰 추출 (현재는 사용하지 않음 - 더미로 처리)
        String jwtToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
        }
        
        try {
            // 입장 검증 및 세션 토큰 발급
            Map<String, Object> sessionData = matchChatAuthService.validateAndCreateSession(jwtToken, request);
            
            // WebSocket 접속 링크 생성
            String websocketUrl = String.format("ws://localhost:8084/ws/match-chat/websocket?token=%s&matchId=%s", 
                    sessionData.get("sessionToken"), request.getMatchId());

            // 응답 DTO 생성
            MatchChatJoinResponse response = MatchChatJoinResponse.builder()
                    .sessionToken((String) sessionData.get("sessionToken"))
                    .userId(((Number) sessionData.get("userId")).longValue())
                    .matchId(String.valueOf(request.getMatchId()))
                    .expiresIn((Long) sessionData.get("expiresIn"))
                    .websocketUrl(websocketUrl)
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
            Map<String, Object> userInfo = matchChatAuthService.getUserInfoByToken(token);
            return ResponseEntity.ok(ApiResponse.success(userInfo));
        } catch (Exception e) {
            log.warn("세션 토큰 검증 실패 - token: {}", token, e);
            throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN);
        }
    }
}