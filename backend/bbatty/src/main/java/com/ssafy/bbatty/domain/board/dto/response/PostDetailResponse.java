package com.ssafy.bbatty.domain.board.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostDetailResponse {
    
    private Long postId;
    private String title;
    private String authorNickname;
    private String content;
    private Integer likeCount;
    private Integer viewCount;
    private Integer commentCount;
    private String createdAt;
    private String updatedAt;
    
}