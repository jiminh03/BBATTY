package com.ssafy.schedule.domain.bestpost.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostEventDto {
    private String eventType;  // VIEW, LIKE, COMMENT
    private Long postId;
    private Long userId;
    private Long teamId;
    private Long commentId;  // 댓글 이벤트인 경우만 사용
    private String timestamp;
}