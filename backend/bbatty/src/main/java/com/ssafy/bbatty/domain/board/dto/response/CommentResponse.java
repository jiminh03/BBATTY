package com.ssafy.bbatty.domain.board.dto.response;

import com.ssafy.bbatty.domain.board.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String nickname;
    private String profileImg;
    private String createdAt;
    private String updatedAt;
    private int depth;
    private List<CommentResponse> replies; // 대댓글 목록

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.nickname = comment.getUser().getNickname();
        this.profileImg = comment.getUser().getProfileImg();
        this.createdAt = comment.getCreatedAt().toString();
        this.updatedAt = comment.getUpdatedAt().toString();
        this.depth = comment.getDepth();
        // replies는 서비스에서 별도로 설정
    }

    public CommentResponse(Comment comment, List<CommentResponse> replies) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.nickname = comment.getUser().getNickname();
        this.profileImg = comment.getUser().getProfileImg();
        this.createdAt = comment.getCreatedAt().toString();
        this.updatedAt = comment.getUpdatedAt().toString();
        this.depth = comment.getDepth();
        this.replies = replies;
    }
}