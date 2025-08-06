package com.ssafy.chat.auth.controller;

import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 채팅 인증 결과 조회 컨트롤러
 * 클라이언트가 bbatty 서버로 인증 요청 후 결과를 폴링으로 확인할 수 있음
 */
@RestController
@RequestMapping("/api/v1/chat/auth")
@RequiredArgsConstructor
@Slf4j
public class ChatAuthResultController {
    
    private final RedisUtil redisUtil;
    
    private static final String AUTH_RESULT_KEY_PREFIX = "chat_auth_result:";
    
    /**
     * 인증 결과 조회 (폴링용)
     */
    @GetMapping("/result/{requestId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthResult(
            @PathVariable String requestId) {
        
        try {
            String resultKey = AUTH_RESULT_KEY_PREFIX + requestId;
            Object result = redisUtil.getValue(resultKey);
            
            if (result == null) {
                return ResponseEntity.ok(ApiResponse.error("인증 결과를 찾을 수 없습니다. 다시 시도해주세요."));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> authResult = (Map<String, Object>) result;
            
            log.debug("인증 결과 조회: requestId={}, success={}", 
                    requestId, authResult.get("success"));
            
            return ResponseEntity.ok(ApiResponse.success(authResult));
            
        } catch (Exception e) {
            log.error("인증 결과 조회 실패: requestId={}", requestId, e);
            return ResponseEntity.ok(ApiResponse.error("인증 결과 조회에 실패했습니다."));
        }
    }
}