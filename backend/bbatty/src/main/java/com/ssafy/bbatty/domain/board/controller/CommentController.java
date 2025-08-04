package com.ssafy.bbatty.domain.board.controller;

import com.ssafy.bbatty.domain.board.dto.request.CommentCreateRequest;
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

    // 4. 댓글 수정
    // 주석 추가
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long id,
            @RequestBody String content
    ) {
        commentService.updateComment(id, content);
        return ResponseEntity.status(SuccessCode.SUCCESS_UPDATED.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_UPDATED));
    }

    // 2. 게시글의 댓글 목록 조회 (대댓글 포함)
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<CommentListResponse>> getCommentsByPostId(@PathVariable Long postId) {
        CommentListResponse response = commentService.getCommentsWithRepliesByPostId(postId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }

    // 5. 댓글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.status(SuccessCode.SUCCESS_DELETED.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DELETED));
    }
}
