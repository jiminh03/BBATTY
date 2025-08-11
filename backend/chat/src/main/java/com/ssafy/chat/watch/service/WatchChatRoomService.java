package com.ssafy.chat.watch.service;

import com.ssafy.chat.watch.dto.ChatRoomCreateEventDto;
import com.ssafy.chat.watch.dto.WatchChatRoom;

import java.util.List;
import java.util.Map;

/**
 * 관전 채팅방 관리 서비스
 * 관전 채팅방의 생성, 조회, 관리 책임을 담당
 */
public interface WatchChatRoomService {

    /**
     * 관전 채팅방 자동 생성 (이벤트 기반)
     */
    String createWatchChatRoom(Long gameId, String roomName, String chatType,
                               Long teamId, ChatRoomCreateEventDto eventDto);
    
    /**
     * 관전 채팅방 수동 생성
     */
    String createWatchChatRoom(Long gameId, Long teamId, String teamName);
    
    /**
     * 특정 게임의 모든 관전 채팅방 조회
     */
    List<WatchChatRoom> getWatchChatRoomsByGame(Long gameId);
    
    /**
     * 특정 관전 채팅방 정보 조회
     */
    WatchChatRoom getWatchChatRoom(String roomId);
    
    /**
     * 활성 관전 채팅방 목록 조회
     */
    Map<String, Object> getActiveWatchChatRooms();
    
    /**
     * 관전 채팅방 비활성화
     */
    void deactivateWatchChatRoom(String roomId);
    
    /**
     * 특정 게임의 모든 관전 채팅방 종료
     */
    void closeWatchChatRoomsForGame(Long gameId);
    
    /**
     * 관전 채팅방 존재 여부 확인
     */
    boolean existsWatchChatRoom(Long gameId, Long teamId);
}
