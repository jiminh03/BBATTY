package com.ssafy.chat.watch.controller;

import com.ssafy.chat.watch.dto.WatchChatJoinRequest;
import com.ssafy.chat.watch.service.WatchChatAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
    public ResponseEntity<Map<String, Object>> joinWatchChat(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody WatchChatJoinRequest request) {
        
        try {
            log.info("직관 채팅방 입장 요청 - gameId: {}, 직관인증: {}", 
                    request.getGameId(), request.isAttendanceVerified());

            String jwtToken = extractTokenFromHeader(authHeader);
            
            Map<String, Object> response = watchChatAuthService.validateAndCreateSession(jwtToken, request);
            
            log.info("직관 채팅방 입장 성공 - gameId: {}, teamId: {}", 
                    request.getGameId(), response.get("teamId"));
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            log.warn("직관 채팅방 입장 인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "error", "UNAUTHORIZED",
                "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("직관 채팅방 입장 요청 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "BAD_REQUEST", 
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("직관 채팅방 입장 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "서버 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 직관 채팅 세션 무효화 (로그아웃)
     */
    @DeleteMapping("/session/{sessionToken}")
    public ResponseEntity<Map<String, Object>> invalidateSession(@PathVariable String sessionToken) {
        try {
            watchChatAuthService.invalidateSession(sessionToken);
            log.info("직관 채팅 세션 무효화 성공 - sessionToken: {}", sessionToken);
            
            return ResponseEntity.ok(Map.of("message", "세션이 무효화되었습니다."));
            
        } catch (Exception e) {
            log.error("세션 무효화 실패 - sessionToken: {}", sessionToken, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "세션 무효화에 실패했습니다."
            ));
        }
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Authorization 헤더가 누락되었거나 형식이 올바르지 않습니다.");
        }
        return authHeader.substring(7);
    }
}