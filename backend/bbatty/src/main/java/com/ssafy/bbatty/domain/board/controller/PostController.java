package com.ssafy.bbatty.domain.board.controller;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.service.PostService;
import com.ssafy.bbatty.domain.board.service.PostImageService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.SuccessCode;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.s3.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader("userId") Long userId) {
        
        PostCreateResponse response = postService.createPost(request, userId);
        
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
            postImageService.saveUploadedImage(fileUrl, null);
            
            return ResponseEntity.status(SuccessCode.SUCCESS_CREATED.getStatus())
                    .body(ApiResponse.success(SuccessCode.SUCCESS_CREATED, fileUrl));
            
        } catch (IOException e) {
            throw new ApiException(ErrorCode.FILE_FAILED);
        }
    }





}