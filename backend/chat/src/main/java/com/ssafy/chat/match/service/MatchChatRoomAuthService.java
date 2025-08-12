package com.ssafy.chat.match.service;

import com.ssafy.chat.common.dto.AuthResult;
import com.ssafy.chat.match.dto.MatchChatJoinRequest;
import com.ssafy.chat.match.dto.MatchChatRoomCreateRequest;

import java.util.Map;

/**
 * 매칭 채팅방 인증 서비스 인터페이스
 */
public interface MatchChatRoomAuthService {
    
    /**
     * 매칭 채팅방 생성 권한 인증
     */
    AuthResult authenticateForCreation(String jwtToken, MatchChatRoomCreateRequest request);
    
    /**
     * 세션 토큰으로 사용자 정보 조회
     */
    Map<String, Object> getUserInfoByToken(String sessionToken);
    
    /**
     * JWT 토큰 검증 및 세션 생성
     */
    Map<String, Object> validateAndCreateSession(String jwtToken, MatchChatJoinRequest request);
}