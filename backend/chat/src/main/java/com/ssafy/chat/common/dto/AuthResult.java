package com.ssafy.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 채팅 인증 결과 DTO
 * Map<String, Object> 대신 타입 안전성을 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {
    
    /**
     * 인증 성공 여부
     */
    private Boolean success;
    
    /**
     * 실패 시 에러 메시지
     */
    private String errorMessage;
    
    /**
     * 인증 성공 시 사용자 정보
     */
    private UserInfo userInfo;
    
    /**
     * 추가 인증 정보 (확장성을 위해)
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 인증 요청 ID
     */
    private String requestId;
    
    /**
     * 인증 처리 시각 (타임스탬프)
     */
    private Long timestamp;
    
    /**
     * 인증 성공 여부 확인
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
    
    /**
     * 인증 실패 여부 확인
     */
    public boolean isFailed() {
        return !isSuccess();
    }
}