package com.ssafy.chat.watch.service;

import com.ssafy.chat.watch.dto.WatchChatJoinRequest;

import java.util.Map;

/**
 * 직관 채팅방 인증 서비스 인터페이스
 * 완전 무명 채팅 - 닉네임 없이 teamId만으로 식별
 */
public interface WatchChatRoomAuthService {
    
    /**
     * JWT 토큰과 직관 인증 정보로 세션 생성
     * @param jwtToken Authorization 헤더의 JWT 토큰 (teamId 포함)
     * @param request 직관 채팅 입장 요청 (gameId, 직관 인증 여부)
     * @return 세션 토큰 및 기본 정보
     */
    Map<String, Object> validateAndCreateSession(String jwtToken, WatchChatJoinRequest request);
    
    /**
     * 세션 토큰으로 사용자 정보 조회
     * @param sessionToken 세션 토큰
     * @return 무명 채팅용 사용자 정보 (teamId, gameId만 포함)
     */
    Map<String, Object> getUserInfoByToken(String sessionToken);
    
    /**
     * 세션 무효화
     * @param sessionToken 무효화할 세션 토큰
     */
    void invalidateSession(String sessionToken);
}