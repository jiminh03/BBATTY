package com.ssafy.chat.auth.service;

import java.util.Map;

/**
 * 채팅 인증 결과 대기 공통 서비스 인터페이스
 */
public interface ChatAuthResultService {
    
    /**
     * 인증 결과 대기
     * @param requestId 요청 ID
     * @param timeoutMs 타임아웃 (밀리초)
     * @return 인증 결과 (타임아웃 시 null)
     */
    Map<String, Object> waitForAuthResult(String requestId, int timeoutMs);
}