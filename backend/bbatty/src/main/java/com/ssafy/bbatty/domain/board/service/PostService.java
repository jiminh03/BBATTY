package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostDetailResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;

public interface PostService {
    
    PostCreateResponse createPost(PostCreateRequest request, Long userId);
    
    void deletePost(Long postId, Long userId);

    PostListPageResponse getPostList(Long cursor);
    
    PostListPageResponse getPostListByTeam(Long teamId, Long cursor);
    
    PostDetailResponse getPostDetail(Long postId, Long userId);
    
    PostListPageResponse getPostListByUser(Long userId, Long cursor);

}