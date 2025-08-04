package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.CommentCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.CommentListPageResponse;
import com.ssafy.bbatty.domain.board.dto.response.CommentListResponse;
import com.ssafy.bbatty.domain.board.entity.Comment;
import org.springframework.stereotype.Service;

import java.util.List;


public interface CommentService {

    // CREATE: 댓글 생성
    Comment createComment(CommentCreateRequest request);

    // READ: 게시글에 달린 댓글 전체 조회
    List<Comment> getCommentsByPostId(Long postId);
    
    // READ: 게시글에 달린 댓글 전체 조회 (대댓글 포함, 응답 DTO)
    CommentListResponse getCommentsWithRepliesByPostId(Long postId);
    
    // READ: 게시글에 달린 댓글 페이지네이션 조회 (대댓글 포함)
    CommentListPageResponse getCommentsWithRepliesByPostIdWithPagination(Long postId, Long cursor);

    // UPDATE: 댓글 수정
    Comment updateComment(Long id, String content);

    // DELETE: 댓글 삭제
    void deleteComment(Long id);
}
