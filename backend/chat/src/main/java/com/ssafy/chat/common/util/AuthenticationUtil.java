package com.ssafy.chat.common.util;

import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 인증 관련 공통 유틸리티 클래스
 */
@Component
@Slf4j
public class AuthenticationUtil {
    
    private static final String BEARER_PREFIX = "Bearer ";
    
    /**
     * Authorization 헤더에서 JWT 토큰 추출
     * 
     * @param authHeader Authorization 헤더 값
     * @return JWT 토큰 문자열
     * @throws ApiException 헤더가 유효하지 않은 경우
     */
    public String extractJwtToken(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            log.warn("Authorization 헤더가 누락됨");
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("잘못된 Authorization 헤더 형식 - Bearer 누락: [{}]", 
                    maskSensitiveData(authHeader));
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        
        if (token.isEmpty()) {
            log.warn("Authorization 헤더에 토큰이 누락됨");
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        
        // 토큰 로깅 시 보안을 위해 일부만 표시
        log.debug("JWT 토큰 추출 성공: [{}...]", 
                token.length() > 20 ? token.substring(0, 20) : token);
        
        return token;
    }
    
    /**
     * Authorization 헤더에서 JWT 토큰 안전하게 추출
     * 예외 대신 null 반환
     * 
     * @param authHeader Authorization 헤더 값
     * @return JWT 토큰 또는 null
     */
    public String extractJwtTokenSafely(String authHeader) {
        try {
            return extractJwtToken(authHeader);
        } catch (ApiException e) {
            return null;
        }
    }
    
    /**
     * 민감한 데이터 마스킹
     * 로깅 시 보안을 위해 사용
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 8) {
            return "[MASKED]";
        }
        return data.substring(0, 4) + "****" + data.substring(data.length() - 4);
    }
    
    /**
     * 토큰이 유효한 형태인지 간단 검증
     * JWT는 일반적으로 3개 부분을 점(.)으로 구분
     */
    public boolean isValidJwtFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = token.split("\\.");
        return parts.length == 3;
    }
}