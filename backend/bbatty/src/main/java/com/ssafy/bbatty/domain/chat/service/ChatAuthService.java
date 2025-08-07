package com.ssafy.bbatty.domain.chat.service;

import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.dto.response.ChatAuthResponse;
import com.ssafy.bbatty.global.response.ApiResponse;

/**
 * 채팅 인증/인가 서비스 인터페이스
 */
public interface ChatAuthService {
    
    /**
     * 채팅방 생성/참여 권한 확인 및 Kafka로 결과 전송
     */
    ApiResponse<ChatAuthResponse> authorizeChatAccess(Long userId, Long userTeamId, String userGender, 
                                                     int userAge, String userNickname, ChatAuthRequest request);
}