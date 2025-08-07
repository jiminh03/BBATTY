package com.ssafy.bbatty.domain.board.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostEventDto {
    
    private Long postId;
    private Long userId;
    private Long teamId;
    private String eventType;
    private LocalDateTime eventTime;
    private Long targetId;
    
    public static PostEventDto createViewEvent(Long postId, Long userId, Long teamId) {
        return PostEventDto.builder()
                .postId(postId)
                .userId(userId)
                .teamId(teamId)
                .eventType("VIEW")
                .eventTime(LocalDateTime.now())
                .build();
    }
    
    public static PostEventDto createLikeEvent(Long postId, Long userId, Long teamId) {
        return PostEventDto.builder()
                .postId(postId)
                .userId(userId)
                .teamId(teamId)
                .eventType("LIKE")
                .eventTime(LocalDateTime.now())
                .build();
    }
    
    public static PostEventDto createCommentEvent(Long postId, Long userId, Long teamId, Long commentId) {
        return PostEventDto.builder()
                .postId(postId)
                .userId(userId)
                .teamId(teamId)
                .eventType("COMMENT")
                .eventTime(LocalDateTime.now())
                .targetId(commentId)
                .build();
    }
}