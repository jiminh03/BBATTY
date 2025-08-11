package com.ssafy.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 세션 토큰 응답 DTO
 * REST API에서 클라이언트에게 반환하는 토큰 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionToken {
    
    /**
     * 세션 토큰 문자열
     */
    private String token;
    
    /**
     * 토큰 만료 시간 (초 단위)
     */
    private Long expiresIn;
    
    /**
     * 토큰 발급 시각 (타임스탬프)
     */
    private Long issuedAt;
    
    /**
     * 토큰 타입 (Bearer 등)
     */
    @Builder.Default
    private String tokenType = "Bearer";
}