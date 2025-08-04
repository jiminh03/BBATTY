package com.ssafy.bbatty.domain.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentListPageResponse {
    private List<CommentResponse> comments;
    private boolean hasNext;
    private Long nextCursor;
}