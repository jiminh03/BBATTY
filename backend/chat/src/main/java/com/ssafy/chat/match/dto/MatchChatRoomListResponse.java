package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 매칭 채팅방 목록 응답 DTO (무한 스크롤용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatRoomListResponse {
    
    private List<com.ssafy.chat.match.dto.MatchChatRoom> chatRooms;
    
    // 다음 페이지 요청에 사용할 cursor
    private String nextCursor;
    
    // 더 가져올 데이터가 있는지
    private boolean hasMore;
    
    // 현재 응답에 포함된 방 개수
    private int count;
    
    // 전체 방 개수
    private int totalCount;
}