package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.MatchChatMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

public interface MatchChatService {
    /**
     * 매칭 채팅방에 사용자 세션 등록
     */
    void addSessionToMatchRoom(String matchId, WebSocketSession session);
    /**
     * 매칭 채팅방에서 사용자 세션 제거
     */
    void removeSessionFromMatchRoom(String matchId, WebSocketSession session);
    /**
     * 채팅 메시지 발송 (with kafka)
     */
    void sendChatMessage(String matchId, MatchChatMessage message);
    /**
     * 사용자 입장 이벤트 발송
     */
    void sendUserJoinEvent(String matchId, String userId, String userName);
    /**
     * 사용자 퇴장 이벤트 발송
     */
    void sendUserLeaveEvent(String matchId, String userId, String userName);
    /**
     * 매칭 채팅방의 최근 메시지 히스토리 조회
     * @param matchId 매칭 Id
     * @param limit 조회할 메시지 수 (기본 50개)
     * @return 최근 메시지 목록
     */
    List<Map<String, Object>> getRecentMessages(String matchId, int limit);
    /**
     * 활성화된 매칭 채팅방 수 반환
     */
    int getActiveMatchRoomCount();
    
    /**
     * 특정 매칭 채팅방의 활성 세션 수 반환
     */
    int getActiveSessionCount(String matchId);

}
