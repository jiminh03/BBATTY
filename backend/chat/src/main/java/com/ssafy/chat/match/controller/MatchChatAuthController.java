package com.ssafy.chat.match.controller;

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
    @PostMapping("/join")
    public ResponseEntity<MatchChatJoinResponse> joinMatchChat(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody MatchChatJoinRequest request) {
        
        try {
            log.info("매칭 채팅방 입장 요청 - matchId: {}, nickname: {}, winRate: {}%", 
                    request.getMatchId(), request.getNickname(), request.getWinRate());
            
            // JWT 토큰 추출 (현재는 사용하지 않음 - 더미로 처리)
            String jwtToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            }
            
            // 입장 검증 및 세션 토큰 발급
            Map<String, Object> sessionData = matchChatAuthService.validateAndCreateSession(jwtToken, request);
            
            // WebSocket 접속 링크 생성
            String websocketUrl = String.format("ws://localhost:8084/ws/match-chat/websocket?token=%s&matchId=%s", 
                    sessionData.get("sessionToken"), request.getMatchId());
            
            // 응답 DTO 생성
            MatchChatJoinResponse response = MatchChatJoinResponse.builder()
                    .sessionToken((String) sessionData.get("sessionToken"))
                    .userId((String) sessionData.get("userId"))
                    .matchId(request.getMatchId())
                    .expiresIn((Long) sessionData.get("expiresIn"))
                    .websocketUrl(websocketUrl)
                    .build();
            
            log.info("매칭 채팅방 입장 승인 - matchId: {}, userId: {}", 
                    request.getMatchId(), response.getUserId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("매칭 채팅방 입장 거부 - matchId: {}, reason: {}", 
                    request.getMatchId(), e.getMessage());
            return ResponseEntity.badRequest().body(null);
            
        } catch (SecurityException e) {
            log.warn("매칭 채팅방 입장 인증 실패 - matchId: {}, reason: {}", 
                    request.getMatchId(), e.getMessage());
            return ResponseEntity.status(403).body(null);
            
        } catch (Exception e) {
            log.error("매칭 채팅방 입장 처리 실패 - matchId: {}", request.getMatchId(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * 세션 토큰 검증 (WebSocket HandshakeInterceptor에서 사용)
     */
    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        try {
            Map<String, Object> userInfo = matchChatAuthService.getUserInfoByToken(token);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.warn("세션 토큰 검증 실패 - token: {}", token, e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "INVALID_TOKEN", "message", "유효하지 않은 토큰입니다."));
        }
    }
}