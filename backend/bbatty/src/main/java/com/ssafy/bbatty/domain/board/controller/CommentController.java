package com.ssafy.bbatty.domain.board.controller;

import com.ssafy.bbatty.domain.board.dto.request.CommentCreateRequest;
import com.ssafy.bbatty.domain.board.dto.request.CommentUpdateRequest;
import com.ssafy.bbatty.domain.board.dto.response.CommentListPageResponse;
import com.ssafy.bbatty.domain.board.dto.response.CommentListResponse;
import com.ssafy.bbatty.domain.board.entity.Comment;
import com.ssafy.bbatty.domain.board.service.CommentService;
import com.ssafy.bbatty.global.constants.SuccessCode;
import com.ssafy.bbatty.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    // 1. 댓글 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createComment(@Valid @RequestBody CommentCreateRequest request) {
        Comment created = commentService.createComment(request);

        return ResponseEntity.status(SuccessCode.SUCCESS_DELETED.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_CREATED));
    }

    // 2. 게시글의 댓글 목록 조회 (대댓글 포함)
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<CommentListResponse>> getCommentsByPostId(@PathVariable Long postId) {
        CommentListResponse response = commentService.getCommentsWithRepliesByPostId(postId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }
    
    // 3. 게시글의 댓글 목록 페이지네이션 조회 (대댓글 포함)
    @GetMapping("/post/{postId}/page")
    public ResponseEntity<CommentListPageResponse> getCommentsByPostIdWithPagination(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor) {
        CommentListPageResponse response = commentService.getCommentsWithRepliesByPostIdWithPagination(postId, cursor);
        return ResponseEntity.ok(response);
    }

    // 5. 댓글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.status(SuccessCode.SUCCESS_DELETED.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DELETED));
    }

    // 4. 댓글 수정
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        commentService.updateComment(id, request.getContent());
        return ResponseEntity.status(SuccessCode.SUCCESS_UPDATED.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_UPDATED));
    }
}
