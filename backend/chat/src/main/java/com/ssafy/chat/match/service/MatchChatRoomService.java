package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.*;

/**
 * 매칭 채팅방 서비스 인터페이스
 */
public interface MatchChatRoomService {
    
    /**
     * 매칭 채팅방 생성
     */
    MatchChatRoomResponse createMatchChatRoom(MatchChatRoomCreateRequest request);
    
    /**
     * 매칭 채팅방 목록 조회 (무한 스크롤)
     */
    MatchChatRoomListResponse getMatchChatRoomList(MatchChatRoomListRequest request);
    
    /**
     * 특정 매칭 채팅방 조회
     */
    MatchChatRoomResponse getMatchChatRoom(String matchId);
}