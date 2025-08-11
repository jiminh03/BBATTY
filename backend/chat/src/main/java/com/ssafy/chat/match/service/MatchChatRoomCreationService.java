package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.MatchChatRoomCreateRequest;
import com.ssafy.chat.match.dto.MatchChatRoomCreateResponse;

/**
 * 매칭 채팅방 생성 서비스
 * 채팅방 생성과 관련된 책임만 담당
 */
public interface MatchChatRoomCreationService {
    
    /**
     * 새로운 매칭 채팅방 생성
     * @param request 채팅방 생성 요청
     * @param jwtToken 사용자 인증 토큰
     * @return 생성된 채팅방 정보
     */
    MatchChatRoomCreateResponse createMatchChatRoom(MatchChatRoomCreateRequest request, String jwtToken);
    
    /**
     * 채팅방 생성 권한 검증
     * @param jwtToken 사용자 인증 토큰
     * @param gameId 게임 ID
     * @return 생성 가능 여부
     */
    boolean canCreateChatRoom(String jwtToken, Long gameId);
    
    /**
     * 중복 채팅방 체크
     * @param gameId 게임 ID
     * @param creatorId 생성자 ID
     * @return 중복 채팅방 존재 여부
     */
    boolean isDuplicateChatRoom(Long gameId, Long creatorId);
}