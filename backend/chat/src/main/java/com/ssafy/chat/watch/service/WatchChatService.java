package com.ssafy.chat.watch.service;

import com.ssafy.chat.watch.dto.WatchChatMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 관전 채팅 서비스 인터페이스
 * Redis Pub/Sub 인프라를 추상화하여 비즈니스 로직을 제공
 */
public interface WatchChatService {
    
    /**
     * 관전 채팅방에 사용자 세션 등록
     */
    void addSessionToWatchRoom(String roomId, WebSocketSession session);
    
    /**
     * 관전 채팅방에서 사용자 세션 제거
     */
    void removeSessionFromWatchRoom(String roomId, WebSocketSession session);
    
    /**
     * 채팅 메시지 발송 (Redis Pub/Sub으로 전송)
     */
    void sendChatMessage(String roomId, WatchChatMessage message);
    
    /**
     * 사용자 입장 이벤트 발송
     */
    void sendUserJoinEvent(String roomId, String userId, String userName);
    
    /**
     * 사용자 퇴장 이벤트 발송
     */
    void sendUserLeaveEvent(String roomId, String userId, String userName);
    
    /**
     * 트래픽 카운트 증가 (급증 모니터링용)
     */
    void incrementTrafficCount(String roomId);
    
    /**
     * 트래픽 급증 감지
     */
    void checkTrafficSpike(String roomId);
    
    /**
     * 활성화된 관전 채팅방 수 반환
     */
    int getActiveWatchRoomCount();
    
    /**
     * 특정 관전 채팅방의 활성 세션 수 반환
     */
    int getActiveSessionCount(String roomId);
    
    /**
     * 사용자 세션 유효성 검증
     */
    boolean validateUserSession(String sessionToken);
    
    /**
     * 세션 토큰에서 사용자 정보 조회
     */
    Map<String, Object> getUserInfoFromSession(String sessionToken);
    
    /**
     * 직관 채팅방 생성 (Redis에 방 정보 저장)
     */
    void createWatchChatRoom(String roomId, Long gameId, Long teamId, String teamName);
}