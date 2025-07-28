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
public class PostListPageResponse {
    private List<PostListResponse> posts;
    private boolean hasNext;
    private Long nextCursor; // 다음 페이지를 위한 커서 (마지막 게시글 ID)

}
