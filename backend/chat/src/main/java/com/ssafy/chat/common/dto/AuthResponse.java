package com.ssafy.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /** 세션 토큰 (Chat 서버에서 생성) */
    private String sessionToken;
    
    /** WebSocket 연결 URL */
    private String websocketUrl;
    
    /** 인증 성공 여부 */
    private boolean success;
    
    /** 오류 메시지 */
    private String errorMessage;
    
    /** 세션 만료 시간 (초) */
    private int expiresIn;
}