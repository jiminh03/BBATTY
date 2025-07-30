package com.ssafy.chat.watch.service;

import com.ssafy.chat.common.dto.ChatSession;
import com.ssafy.chat.common.dto.ChatSessionResponse;


/**
 * 직관(Watch) 채팅 인증 서비스 인터페이스
 */

public interface WatchAuthService {
    /**
     * 직관 채팅 세션 토큰 생성 (조건 불만족시 예외 발생)
     * @param jwtToken JWT 토큰
     * @param nickname 클라이언트 닉네임
     * @param attandanceAuth 직관 인증 여부
     * @param watchChatRoomId 직관 채팅방 ID
     * @return 채팅 세션 응답
     * @throws RuntimeException 입장 조건 불만족
     */
    ChatSessionResponse createWatchChatSession(String jwtToken, String nickname, Boolean attandanceAuth, String watchChatRoomId);

    /**
     * 세션 토큰 검증 (WebScoket handshake 용)
     * @param sessionToken 세션 토큰
     * @return 채팅 세션 정보
     * @throws RuntimeException 인증 실패
     */
    ChatSession validateWatchChatSession(String sessionToken, String nickname);

}
