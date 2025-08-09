package com.ssafy.bbatty.domain.chat.strategy;

import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;

/**
 * 채팅 유형별 검증 전략 인터페이스
 */
public interface ChatValidationStrategy {
    
    /**
     * 채팅 권한 검증
     */
    void validateChatPermission(Long userId, Long userTeamId, String userGender, int userAge, ChatAuthRequest request);
    
    /**
     * 전략이 지원하는 채팅 유형 반환
     */
    String getSupportedChatType();
}