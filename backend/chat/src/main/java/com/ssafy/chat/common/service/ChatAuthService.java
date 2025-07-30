package com.ssafy.chat.common.service;

import com.ssafy.chat.common.dto.ChatSession;
import com.ssafy.chat.common.dto.ChatSessionResponse;

/**
 * 채팅 인증 서비스 인터페이스
 */
public interface ChatAuthService {

    /**
     * JWT 토큰 검증 및 세션 토큰 생성
     * @param jwtToken JWT 토큰
     * @param nickname 클라이언트 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @param winRate 승률
     * @param chatRoomId 채팅방 ID
     * @param attendanceAuth 직관 인증 여부 (직관 채팅용, 매칭 채팅은 null)
     * @return 채팅 세션 응답
     */
    ChatSessionResponse createChatSession(String jwtToken, String nickname, String profileImageUrl, 
                                        Integer winRate, String chatRoomId, Boolean attendanceAuth);

    /**
     * 세션 토큰 검증
     * @param sessionToken 세션 토큰
     * @return 채팅 세션 정보
     */
    ChatSession validateSessionToken(String sessionToken);

    /**
     * 직관 채팅 입장 조건 검증 (같은 팀만)
     * @param jwtTeamId JWT의 팀 ID
     * @param chatRoomId 채팅방 ID
     * @return 입장 가능 여부
     */
    boolean canJoinWatchChat(String jwtTeamId, String chatRoomId);

    /**
     * 매칭 채팅 입장 조건 검증 (복잡한 조건들)
     * @param jwtUserId JWT의 사용자 ID
     * @param jwtTeamId JWT의 팀 ID
     * @param jwtGender JWT의 성별
     * @param jwtAge JWT의 나이
     * @param winRate 클라이언트 승률
     * @param chatRoomId 매칭 채팅방 ID
     * @return 입장 가능 여부
     */
    boolean canJoinMatchChat(String jwtUserId, String jwtTeamId, String jwtGender, 
                           Integer jwtAge, Integer winRate, String chatRoomId);
}