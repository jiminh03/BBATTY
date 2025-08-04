package com.ssafy.bbatty.domain.board.dto.response;

import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostListResponse {
    private Long id;
    private String title;
    private String nickname;
    private String createdAt;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;

    public PostListResponse(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.nickname = post.getUser().getNickname();
        this.createdAt = post.getCreatedAt().toString();
    }
}
