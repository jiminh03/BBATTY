package com.ssafy.bbatty.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateRequest {

    @NotNull
    private Long postId;

    @NotNull
    private Long userId;

    @NotBlank
    private String content;

    private Long parentId;

}
