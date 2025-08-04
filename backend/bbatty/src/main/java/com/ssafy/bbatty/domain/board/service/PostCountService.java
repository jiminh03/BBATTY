package com.ssafy.bbatty.domain.board.service;

public interface PostCountService {
    
    Integer getViewCount(Long postId);
    
    Integer getLikeCount(Long postId);
    
    void incrementViewCount(Long postId, Long userId);

    void incrementLikeCount(Long postId, Long userId);

    void decrementLikeCount(Long postId, Long userId);
    
    void refreshCountsFromDB(Long postId);
    
    Integer getCommentCount(Long postId);
}