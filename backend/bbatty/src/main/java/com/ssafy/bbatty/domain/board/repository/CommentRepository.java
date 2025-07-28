package com.ssafy.bbatty.domain.board.repository;

import com.ssafy.bbatty.domain.board.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 특정 게시글의 댓글 조회 (부모 댓글만, depth=0)
    List<Comment> findByPostIdAndDepthOrderByCreatedAtAsc(Long postId, int depth);
    
    // 특정 부모 댓글의 대댓글 조회 (depth=1)
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
