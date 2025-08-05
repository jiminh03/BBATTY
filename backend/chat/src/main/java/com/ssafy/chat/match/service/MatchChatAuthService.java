package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.MatchChatJoinRequest;

import java.util.Map;

/**
 * 매칭 채팅 인증 서비스
 * JWT 토큰 검증 및 매칭방 입장 조건 확인
 */
public interface MatchChatAuthService {
    
    /**
     * 매칭 채팅방 입장 검증 및 세션 생성
     * @param jwtToken JWT 토큰 (userId, 성별, 나이, 팀정보 포함)
     * @param request 클라이언트 정보 (닉네임, 승률, 프사URL, 승리요정여부)
     * @return 세션 토큰 및 사용자 정보
     */
    Map<String, Object> validateAndCreateSession(String jwtToken, MatchChatJoinRequest request);
    
    /**
     * 세션 토큰으로 사용자 정보 조회
     * @param sessionToken 세션 토큰
     * @return 사용자 정보
     */
    Map<String, Object> getUserInfoByToken(String sessionToken);
    
    /**
     * 세션 토큰 무효화
     * @param sessionToken 세션 토큰
     */
    void invalidateSession(String sessionToken);
}