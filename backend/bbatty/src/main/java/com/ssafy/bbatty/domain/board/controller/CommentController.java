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

        return ResponseEntity.status(SuccessCode.SUCCESS_CREATED.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_CREATED));
    }

    // 2. 댓글 수정
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long id,
            @RequestBody String content
    ) {
        commentService.updateComment(id, content);
        return ResponseEntity.status(SuccessCode.SUCCESS_UPDATED.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_UPDATED));
    }

    // 3. 게시글의 댓글 목록 페이지네이션 조회 (대댓글 포함)
    @GetMapping("/post/{postId}/page")
    public ResponseEntity<ApiResponse<CommentListPageResponse>> getCommentsByPostIdWithPagination(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor) {
        CommentListPageResponse response = commentService.getCommentsWithRepliesByPostIdWithPagination(postId, cursor);
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }

    // 6. 댓글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.status(SuccessCode.SUCCESS_DELETED.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DELETED));
    }
}
