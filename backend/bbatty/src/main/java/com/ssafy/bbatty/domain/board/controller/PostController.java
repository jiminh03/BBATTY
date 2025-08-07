package com.ssafy.bbatty.domain.board.controller;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostDetailResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;
import com.ssafy.bbatty.domain.board.service.PostCountService;
import com.ssafy.bbatty.domain.board.service.PostService;
import com.ssafy.bbatty.domain.board.service.PostImageService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.SuccessCode;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.s3.S3Service;
import com.ssafy.bbatty.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final S3Service s3Service;
    private final PostCountService postCountService;
    private final PostImageService postImageService;

    // 게시물 생성
    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PostCreateResponse response = postService.createPost(request, userPrincipal.getUserId());
        return ResponseEntity.status(SuccessCode.SUCCESS_CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_CREATED, response));
    }

    // 게시물 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        postService.deletePost(postId, userPrincipal.getUserId());
        return ResponseEntity.status(SuccessCode.SUCCESS_DELETED.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DELETED));
    }

    // 전체 게시물 목록 조회 - 응답 형식
    @GetMapping
    public ResponseEntity<ApiResponse<PostListPageResponse>> getPostList(
            @RequestParam(required = false) Long cursor) {

        PostListPageResponse response = postService.getPostList(cursor);
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }

    // 팀 별 게시글 목록 조회
    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<PostListPageResponse>> getPostListByTeam(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long cursor) {

        PostListPageResponse response = postService.getPostListByTeam(teamId, cursor);
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        PostDetailResponse response = postService.getPostDetail(postId, userPrincipal.getUserId());
        
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }

    // 사용자에 대한 게시물 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PostListPageResponse>> getPostListByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Long cursor) {

        PostListPageResponse response = postService.getPostListByUser(userId, cursor);
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }

    //게시글 좋아요 (앱에서 사용자 좋아요 기록은 따로 저장)
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId,
                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        // 좋아요 수 증가 (Redis에만 증가 되고 나중에 RDB에 반영된다.)
        postCountService.incrementLikeCount(postId, userPrincipal.getUserId());

        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT));
    }

    /**
     * 게시글 좋아요 취소 (프론트에서 현재 좋아요 수 전달)
     */
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<?> unlikePost(@PathVariable Long postId,
                                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

        postCountService.decrementLikeCount(postId, userPrincipal.getUserId());

        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT));
    }

    /**
     * 이미지 업로드를 위한 Presigned URL 생성
     */
    @PostMapping("/images/presigned-url")
    public ResponseEntity<ApiResponse<S3Service.PresignedUrlResponse>> generatePresignedUrl(
            @RequestParam String filename,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            S3Service.PresignedUrlResponse response = s3Service.generatePresignedUploadUrlWithPath("posts", filename);
            
            return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                    .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_FILE_PATH);
        }
    }

    /**
     * 게시글의 특정 이미지 삭제 (소프트 삭제)
     */
    @DeleteMapping("/{postId}/images")
    public ResponseEntity<ApiResponse<Void>> deletePostImage(
            @PathVariable Long postId,
            @RequestParam String imageUrl,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            postImageService.softDeleteImageByPostAndUrl(postId, imageUrl);
            
            return ResponseEntity.status(SuccessCode.SUCCESS_DELETED.getStatus())
                    .body(ApiResponse.success(SuccessCode.SUCCESS_DELETED));
        } catch (RuntimeException e) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
    }

}