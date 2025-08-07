package com.ssafy.chat.watch.controller;

import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.global.response.ApiResponse;
import com.ssafy.chat.watch.dto.WatchChatJoinRequest;
import com.ssafy.chat.watch.dto.WatchChatJoinResponse;
import com.ssafy.chat.watch.service.WatchChatAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 직관 채팅 인증 컨트롤러
 * 완전 무명 채팅 - teamId 기반 입장 관리
 */
@RestController
@RequestMapping("/api/watch-chat")
@RequiredArgsConstructor
@Slf4j
public class WatchChatAuthController {

    private final WatchChatAuthService watchChatAuthService;

    /**
     * 직관 채팅방 입장 토큰 발급
     * JWT에서 teamId 추출, 직관 인증 여부 확인 후 세션 토큰 생성
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<WatchChatJoinResponse>> joinWatchChat(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody WatchChatJoinRequest request) {
        
        log.info("직관 채팅방 입장 요청 - gameId: {}, 직관인증: {}", 
                request.getGameId(), request.isAttendanceVerified());

        // 현재는 JWT 토큰 사용하지 않음 - 더미로 처리
        String jwtToken = null;
        
        try {
            Map<String, Object> sessionData = watchChatAuthService.validateAndCreateSession(jwtToken, request);
            
            // WebSocket 접속 링크 생성
            String websocketUrl = String.format("ws://localhost:8084/ws/watch-chat/websocket?token=%s&gameId=%s", 
                    sessionData.get("sessionToken"), request.getGameId());
            
            // 응답 DTO 생성
            WatchChatJoinResponse response = WatchChatJoinResponse.builder()
                    .sessionToken((String) sessionData.get("sessionToken"))
                    .teamId((Long) sessionData.get("teamId"))
                    .gameId(request.getGameId())
                    .expiresIn((Long) sessionData.get("expiresIn"))
                    .websocketUrl(websocketUrl)
                    .build();
            
            log.info("직관 채팅방 입장 성공 - gameId: {}, teamId: {}", 
                    request.getGameId(), response.getTeamId());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (SecurityException e) {
            log.warn("직관 채팅방 입장 인증 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.UNAUTHORIZED, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("직관 채팅방 입장 요청 오류: {}", e.getMessage());
            throw new ApiException(ErrorCode.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 직관 채팅 세션 무효화 (로그아웃)
     */
    @DeleteMapping("/session/{sessionToken}")
    public ResponseEntity<ApiResponse<Void>> invalidateSession(@PathVariable String sessionToken) {
        try {
            watchChatAuthService.invalidateSession(sessionToken);
            log.info("직관 채팅 세션 무효화 성공 - sessionToken: {}", sessionToken);
            
            return ResponseEntity.ok(ApiResponse.success());
            
        } catch (Exception e) {
            log.error("세션 무효화 실패 - sessionToken: {}", sessionToken, e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "세션 무효화에 실패했습니다.");
        }
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Authorization 헤더가 누락되었거나 형식이 올바르지 않습니다.");
        }
        return authHeader.substring(7);
    }
}