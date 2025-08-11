package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.MatchChatRoomListRequest;
import com.ssafy.chat.match.dto.MatchChatRoomListResponse;

/**
 * 매칭 채팅방 조회 서비스
 * 채팅방 목록 조회, 검색과 관련된 책임만 담당
 */
public interface MatchChatRoomQueryService {
    
    /**
     * 매칭 채팅방 목록 조회
     * @param request 조회 요청 (페이징, 필터 조건 포함)
     * @return 채팅방 목록 응답
     */
    MatchChatRoomListResponse getMatchChatRoomList(MatchChatRoomListRequest request);
    
    /**
     * 키워드로 채팅방 검색
     * @param request 검색 요청 (키워드 포함)
     * @return 검색 결과 목록
     */
    MatchChatRoomListResponse searchMatchChatRooms(MatchChatRoomListRequest request);
    
    /**
     * 특정 채팅방 상세 정보 조회
     * @param matchId 매칭 채팅방 ID
     * @return 채팅방 상세 정보
     */
    com.ssafy.chat.match.dto.MatchChatRoom getMatchChatRoomDetail(String matchId);
    
    /**
     * 사용자가 참여 가능한 채팅방 필터링
     * @param request 조회 요청
     * @param userId 사용자 ID  
     * @param userAge 사용자 나이
     * @param userGender 사용자 성별
     * @return 참여 가능한 채팅방 목록
     */
    MatchChatRoomListResponse getJoinableChatRooms(
            MatchChatRoomListRequest request, 
            Long userId, 
            Integer userAge, 
            String userGender);
}