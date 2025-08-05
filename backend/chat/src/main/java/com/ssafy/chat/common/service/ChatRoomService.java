package com.ssafy.chat.common.service;

import java.util.List;
import java.util.Map;

/**
 * 채팅방 관리 서비스 인터페이스
 * 비즈니스 로직을 담당
 */
public interface ChatRoomService {
    
    /**
     * 사용자 채팅방 입장 처리
     * @param roomId 채팅방 ID
     * @param sessionId WebSocket 세션 ID
     * @param userInfo 사용자 정보
     */
    void enterRoom(String roomId, String sessionId, Map<String, Object> userInfo);
    
    /**
     * 사용자 채팅방 퇴장 처리
     * @param sessionId WebSocket 세션 ID
     */
    void leaveRoom(String sessionId);
    
    /**
     * 채팅방 사용자 목록 조회
     * @param roomId 채팅방 ID
     * @return 사용자 목록
     */
    List<Map<String, Object>> getRoomUsers(String roomId);
    
    /**
     * 채팅방 사용자 수 조회
     * @param roomId 채팅방 ID
     * @return 사용자 수
     */
    int getRoomUserCount(String roomId);
    
    /**
     * 세션으로 채팅방 정보 조회
     * @param sessionId WebSocket 세션 ID
     * @return 채팅방 ID (없으면 null)
     */
    String getRoomBySession(String sessionId);
    
    /**
     * 채팅방 생성 및 정보 저장
     * @param roomId 채팅방 ID
     * @param roomType 채팅방 타입 (match/watch)
     * @param additionalInfo 추가 정보
     */
    void createRoom(String roomId, String roomType, Map<String, Object> additionalInfo);
}