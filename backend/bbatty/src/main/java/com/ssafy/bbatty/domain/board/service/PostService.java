package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;

public interface PostService {
    
    PostCreateResponse createPost(PostCreateRequest request, Long userId);
    
    void deletePost(Long postId, Long userId);
    
}