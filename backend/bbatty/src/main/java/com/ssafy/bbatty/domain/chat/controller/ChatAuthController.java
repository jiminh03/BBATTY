package com.ssafy.bbatty.domain.chat.controller;

import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.dto.response.ChatAuthResponse;
import com.ssafy.bbatty.domain.chat.service.ChatAuthService;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 채팅 인증/인가 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/chat/auth")
@RequiredArgsConstructor
@Slf4j
public class ChatAuthController {
    
    private final ChatAuthService chatAuthService;
    
    /**
     * 채팅방 생성/참여 인증
     */
    @PostMapping("/authorize")
    public ResponseEntity<ApiResponse<ChatAuthResponse>> authorizeChatAccess(
            @RequestBody ChatAuthRequest request) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("채팅 인증 요청: userId={}, chatType={}, action={}, requestId={}", 
                userId, request.getChatType(), request.getAction(), request.getRequestId());
        
        // 컨트롤러에서는 기본값으로 처리 (실제로는 Kafka Consumer를 통해 호출됨)
        ApiResponse<ChatAuthResponse> response = chatAuthService.authorizeChatAccess(userId, null, null, 0, null, request);
        return ResponseEntity.ok(response);
    }
}