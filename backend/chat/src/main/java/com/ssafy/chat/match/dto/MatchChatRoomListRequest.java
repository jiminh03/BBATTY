package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 매칭 채팅방 목록 조회 요청 DTO (무한 스크롤용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatRoomListRequest {
    
    // 검색 키워드 (제목, 설명에서 검색)
    private String keyword;
    
    // 마지막으로 받은 매칭방의 생성시간 (cursor)
    private String lastCreatedAt;
    
    // 한 번에 가져올 개수 (기본 50개)
    @Min(value = 1, message = "최소 1개 이상이어야 합니다")
    @Max(value = 100, message = "최대 100개까지 가능합니다") 
    @Builder.Default
    private int limit = 50;
}