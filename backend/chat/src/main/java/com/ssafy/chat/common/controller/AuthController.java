package com.ssafy.chat.common.controller;

import com.ssafy.chat.common.dto.AuthRequest;
import com.ssafy.chat.common.dto.AuthResponse;
import com.ssafy.chat.common.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 채팅 인증 컨트롤러
 */
@RestController
@RequestMapping("/api/chat/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 채팅 인증 및 세션 생성 (d)
     */
    @PostMapping("/session")
    public ResponseEntity<AuthResponse> createChatSession(@RequestBody AuthRequest authRequest) {
        try {
            log.info("채팅 세션 생성 요청 - chatType: {}, roomId: {}", 
                    authRequest.getChatType(), authRequest.getRoomId());

            AuthResponse response = authService.authenticateAndCreateSession(authRequest);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("채팅 세션 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(AuthResponse.builder()
                            .success(false)
                            .errorMessage("Internal server error")
                            .build());
        }
    }

    /**
     * 세션 무효화
     */
    @DeleteMapping("/session/{sessionToken}")
    public ResponseEntity<Void> invalidateSession(@PathVariable String sessionToken) {
        try {
            authService.invalidateSession(sessionToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("세션 무효화 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Chat Auth Service is running");
    }
}