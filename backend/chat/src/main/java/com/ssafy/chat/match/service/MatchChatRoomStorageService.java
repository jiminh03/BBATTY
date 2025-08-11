package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.MatchChatRoom;

import java.util.List;

/**
 * 매칭 채팅방 저장소 서비스 인터페이스
 */
public interface MatchChatRoomStorageService {
    
    /**
     * 매칭 채팅방을 Redis에 저장
     */
    void saveMatchChatRoom(MatchChatRoom chatRoom, String gameDate);
    
    /**
     * 특정 게임의 매칭 채팅방 목록 조회
     */
    List<MatchChatRoom> getMatchChatRoomsByGameId(Long gameId, int offset, int limit);
    
    /**
     * 키워드로 매칭 채팅방 검색
     */
    List<MatchChatRoom> searchMatchChatRoomsByKeyword(String keyword, int offset, int limit);
    
    /**
     * 매칭 채팅방 조회
     */
    MatchChatRoom getMatchChatRoom(String matchId);
    
    /**
     * 매칭 채팅방 존재 여부 확인
     */
    boolean existsMatchChatRoom(String matchId);
}