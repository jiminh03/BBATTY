package com.ssafy.chat.watch.service;

import java.util.List;

/**
 * 게임 채팅 사용자 관리 서비스 인터페이스
 */
public interface GameChatUserService {
    
    /**
     * 사용자 추가
     */
    void addUser(String teamId, String userId, String userName);
    
    /**
     * 사용자 제거
     */
    void removeUser(String teamId, String userId);
    
    /**
     * 연결된 사용자 수 조회
     */
    long getConnectedUserCount(String teamId);
    
    /**
     * 연결된 사용자 목록 조회
     */
    List<String> getConnectedUsers(String teamId);
    
    /**
     * 사용자 활동 시간 업데이트
     */
    void updateUserActivity(String teamId, String userId, String userName);
}