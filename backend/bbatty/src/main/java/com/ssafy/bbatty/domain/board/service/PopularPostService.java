package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;

public interface PopularPostService {
    
    /**
     * 팀별 인기글 커서 기반 페이징 조회
     */
    PostListPageResponse getPopularPostsByTeam(Long teamId, Long cursor);
}