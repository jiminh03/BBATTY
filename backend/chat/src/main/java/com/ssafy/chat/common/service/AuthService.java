package com.ssafy.chat.common.service;

import com.ssafy.chat.common.dto.AuthRequest;
import com.ssafy.chat.common.dto.AuthResponse;

/**
 * 인증 서비스
 */
public interface AuthService {
    
    /**
     * 채팅 인증 및 세션 생성
     * @param authRequest 인증 요청
     * @return 인증 결과 및 세션 토큰
     */
    AuthResponse authenticateAndCreateSession(AuthRequest authRequest);
    
    /**
     * 세션 토큰 무효화
     * @param sessionToken 세션 토큰
     */
    void invalidateSession(String sessionToken);
}