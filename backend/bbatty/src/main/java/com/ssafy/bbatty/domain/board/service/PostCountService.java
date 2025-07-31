package com.ssafy.bbatty.domain.board.service;

public interface PostCountService {
    
    Long getViewCount(Long postId);
    
    Long getLikeCount(Long postId);
    
    void incrementViewCount(Long postId, Long userId);

    void incrementLikeCount(Long postId, Long userId);

    void decrementLikeCount(Long postId, Long userId);
    
    void refreshCountsFromDB(Long postId);
    
    Long getCommentCount(Long postId);
}