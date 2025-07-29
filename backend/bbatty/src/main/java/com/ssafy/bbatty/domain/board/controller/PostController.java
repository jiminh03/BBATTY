package com.ssafy.bbatty.domain.board.controller;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostDetailResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final S3Service s3Service;
    private final PostImageService postImageService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        PostCreateResponse response = postService.createPost(request, userPrincipal.getUserId());
        
        return ResponseEntity.status(SuccessCode.SUCCESS_CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_CREATED, response));
    }
    /**
     * 게시글에 첨부할 이미지 파일을 업로드합니다.
     *
     * 요청 본문은 Multipart/Form-Data 형식이어야 하며,
     * 하나 이상의 이미지 파일을 포함해야 합니다.
     *
     * <p>업로드된 파일은 S3 등 외부 스토리지에 저장되며,
     * 저장된 이미지 URL이 응답으로 반환됩니다.
     *
     * @return 업로드된 이미지의 URL 목록
     */
    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @RequestParam("file") MultipartFile file) {

        try {
            // 파일 존재 검증
            if (file.isEmpty()) {
                throw new ApiException(ErrorCode.FILE_EMPTY);
            }

            // 파일 크기 검증 (10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new ApiException(ErrorCode.FILE_SIZE_EXCEEDED);
            }

            String filePath = s3Service.uploadFile(file, "posts");
            String fileUrl = s3Service.getPublicUrl(filePath);
            
            // PostImage 엔티티에 UPLOADED 상태로 임시 저장 (post_id는 null)
            postImageService.saveUploadedImage(fileUrl);
            
            return ResponseEntity.status(SuccessCode.SUCCESS_CREATED.getStatus())
                    .body(ApiResponse.success(SuccessCode.SUCCESS_CREATED, fileUrl));
            
        } catch (IOException e) {
            throw new ApiException(ErrorCode.FILE_FAILED);
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        postService.deletePost(postId, userPrincipal.getUserId());
        
        return ResponseEntity.status(SuccessCode.SUCCESS_DELETED.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DELETED));
    }

    @GetMapping
    public ResponseEntity<PostListPageResponse> getPostList(
            @RequestParam(required = false) Long cursor) {

        PostListPageResponse response = postService.getPostList(cursor);
        return ResponseEntity.ok(response);
    }

    // 팀 별 게시글 조회
    @GetMapping("/team/{teamId}")
    public ResponseEntity<PostListPageResponse> getPostListByTeam(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long cursor) {

        PostListPageResponse response = postService.getPostListByTeam(teamId, cursor);
        return ResponseEntity.ok(response);
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable Long postId) {

        PostDetailResponse response = postService.getPostDetail(postId);
        
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PostListPageResponse>> getPostListByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Long cursor) {

        PostListPageResponse response = postService.getPostListByUser(userId, cursor);
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
    }

    /**
     * 게시글 좋아요 (앱에서 사용자 좋아요 기록은 따로 저장)
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId) {
        // 좋아요 수 증가 (Redis에만 증가 되고 나중에 RDB에 반영된다.)
        postService.incrementLikeCount(postId);

        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT));
    }

    /**
     * 게시글 좋아요 취소 (프론트에서 현재 좋아요 수 전달)
     */
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<?> unlikePost(@PathVariable Long postId) {

        // 좋아요 수 감소 (Redis에만 감소 되고 나중에 RDB에 반영된다.)
        postService.decrementLikeCount(postId);

        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT));
    }


}