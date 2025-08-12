package com.ssafy.chat.common.service;

import com.ssafy.chat.common.dto.SessionInfo;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Set;

/**
 * 분산 환경에서 WebSocket 세션을 관리하는 서비스 인터페이스
 * Redis를 통한 중앙화된 세션 정보 관리
 */
public interface DistributedSessionManagerService {
    
    // ===========================================
    // 세션 등록 및 해제
    // ===========================================
    
    /**
     * 세션을 특정 채팅방에 등록
     * @param roomId 채팅방 ID
     * @param session WebSocket 세션
     * @param sessionInfo 세션 정보
     */
    void registerSession(String roomId, WebSocketSession session, SessionInfo sessionInfo);
    
    /**
     * 세션을 특정 채팅방에서 해제
     * @param roomId 채팅방 ID
     * @param sessionId 세션 ID
     */
    void unregisterSession(String roomId, String sessionId);
    
    /**
     * 세션을 모든 채팅방에서 해제
     * @param sessionId 세션 ID
     */
    void unregisterSessionFromAllRooms(String sessionId);
    
    // ===========================================
    // 세션 조회
    // ===========================================
    
    /**
     * 특정 채팅방의 활성 세션 목록 조회
     * @param roomId 채팅방 ID
     * @return 활성 세션 정보 목록
     */
    List<SessionInfo> getActiveSessionsInRoom(String roomId);
    
    /**
     * 특정 채팅방의 활성 세션 수 조회
     * @param roomId 채팅방 ID
     * @return 활성 세션 수
     */
    int getActiveSessionCount(String roomId);
    
    /**
     * 특정 서버 인스턴스의 활성 세션 목록 조회
     * @param instanceId 서버 인스턴스 ID
     * @return 활성 세션 정보 목록
     */
    List<SessionInfo> getActiveSessionsByInstance(String instanceId);
    
    /**
     * 전체 활성 채팅방 목록 조회
     * @return 활성 채팅방 ID 목록
     */
    Set<String> getActiveRooms();
    
    // ===========================================
    // 세션 상태 관리
    // ===========================================
    
    /**
     * 세션 하트비트 업데이트
     * @param sessionId 세션 ID
     */
    void updateSessionHeartbeat(String sessionId);
    
    /**
     * 비활성 세션 정리
     * @return 정리된 세션 수
     */
    int cleanupInactiveSessions();
    
    /**
     * 특정 서버 인스턴스의 모든 세션 정리 (서버 종료 시)
     * @param instanceId 서버 인스턴스 ID
     * @return 정리된 세션 수
     */
    int cleanupInstanceSessions(String instanceId);
    
    /**
     * 특정 채팅방의 모든 세션 정리 (채팅방 종료 시)
     * @param roomId 채팅방 ID
     * @return 정리된 세션 수
     */
    int cleanupRoomSessions(String roomId);
    
    // ===========================================
    // 분산 메시지 브로드캐스트
    // ===========================================
    
    /**
     * 특정 채팅방의 모든 인스턴스에 메시지 브로드캐스트
     * @param roomId 채팅방 ID
     * @param message 메시지 내용
     * @param excludeInstanceId 제외할 인스턴스 ID (자기 자신)
     */
    void broadcastToRoom(String roomId, String message, String excludeInstanceId);
    
    /**
     * 특정 세션에만 메시지 전송
     * @param sessionId 세션 ID
     * @param message 메시지 내용
     */
    void sendToSession(String sessionId, String message);
    
    // ===========================================
    // 모니터링 및 통계
    // ===========================================
    
    /**
     * 전체 활성 세션 수 조회
     * @return 전체 활성 세션 수
     */
    int getTotalActiveSessionCount();
    
    /**
     * 전체 활성 채팅방 수 조회
     * @return 전체 활성 채팅방 수
     */
    int getTotalActiveRoomCount();
    
    /**
     * 인스턴스별 세션 분포 조회
     * @return 인스턴스 ID -> 세션 수 맵
     */
    java.util.Map<String, Integer> getSessionDistributionByInstance();
}