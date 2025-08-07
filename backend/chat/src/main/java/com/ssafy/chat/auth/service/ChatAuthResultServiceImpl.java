package com.ssafy.chat.auth.service;

import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 채팅 인증 결과 대기 공통 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAuthResultServiceImpl implements ChatAuthResultService {
    
    private final RedisUtil redisUtil;
    
    @Override
    public Map<String, Object> waitForAuthResult(String requestId, int timeoutMs) {
        String resultKey = "chat_auth_result:" + requestId;
        int pollingInterval = 500; // 0.5초마다 폴링
        int maxAttempts = timeoutMs / pollingInterval;
        
        log.debug("인증 결과 대기 시작: requestId={}, timeout={}ms", requestId, timeoutMs);
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                Object result = redisUtil.getValue(resultKey);
                if (result != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> authResult = (Map<String, Object>) result;
                    
                    log.debug("인증 결과 수신: requestId={}, attempt={}", requestId, attempt + 1);
                    
                    // 결과 사용 후 Redis에서 제거
                    redisUtil.deleteKey(resultKey);
                    
                    return authResult;
                }
                
                Thread.sleep(pollingInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("인증 대기 중 인터럽트 발생: requestId={}", requestId);
                throw new ApiException(ErrorCode.SERVER_ERROR, "인증 대기 중 인터럽트 발생");
            }
        }
        
        log.warn("인증 결과 타임아웃: requestId={}, timeout={}ms", requestId, timeoutMs);
        return null; // 타임아웃
    }
}