package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.entity.PostImage;
import com.ssafy.bbatty.domain.board.repository.PostImageRepository;
import com.ssafy.bbatty.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostImageService {
    
    private final PostImageRepository postImageRepository;
    private final S3Service s3Service;

    // S3 URL 패턴 (도메인 부분을 유연하게 처리)
    // 게시글 내용에서 이미지 URL을 추출하기 위한 정규표현식 패턴
    // https://도메인/posts/UUID형태파일명.(jpg|jpeg|png|gif|webp) 형식의 URL만 매칭
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
        "https://[^\\s/]+/posts/[a-f0-9-]+\\.(jpg|jpeg|png|gif|webp)",
        Pattern.CASE_INSENSITIVE
    );

    /*
    이미지 업로드 시 DB에 저장하는 메서드
    처음 업로드할 때는 post가 null로 저장됨 (나중에 게시글 작성 시 연결)
     */
    @Transactional
    public PostImage saveUploadedImage(String imageUrl) {
        PostImage postImage = PostImage.builder()
                .post(null)  // 초기 업로드 시에는 post 연결 안됨
                .imageUrl(imageUrl)
                .build();
        return postImageRepository.save(postImage);
    }

    /*
    게시글 내용에 포함된 이미지들을 해당 게시글과 연결하는 메서드
    postId가 null인 이미지들을 찾아서 post와 연결
     */
    @Transactional
    public void processImagesInContent(String content, Post post) {
        // 이미지 url을 content에서 추출한다.
        List<String> imageUrls = extractImageUrls(content);
        // url과 post를 postImage 테이블에 저장
        for (String url : imageUrls) {
            PostImage postImage = PostImage.builder()
                    .post(post)
                    .imageUrl(url)
                    .build();
            postImageRepository.save(postImage);
        }
    }
    
    /*
    이미지 url을 찾아내서 리스트로 받는다.
     */
    public List<String> extractImageUrls(String content) {
        return IMAGE_URL_PATTERN.matcher(content)
                .results()
                .map(matchResult -> matchResult.group())
                .collect(Collectors.toList());
    }
    
    /*
    게시글 삭제 시 연관된 이미지들을 소프트 삭제하는 메서드
    */
    @Transactional
    public void softDeleteImagesForPost(Long postId) {
        List<PostImage> postImages = postImageRepository.findByPostId(postId);
        if (!postImages.isEmpty()) {
            postImages.forEach(postImage -> {
                postImage.setIsDeleted(true);
                postImageRepository.save(postImage);
            });
            log.info("Soft deleted {} images for post {}", postImages.size(), postId);
        }
    }
    
    /*
    게시글 삭제 시 연관된 이미지들을 S3에서 먼저 삭제하는 메서드
    */
    @Transactional
    public void deleteImagesForPost(Long postId) {
        List<String> imageUrls = postImageRepository.findImageUrlsByPostId(postId);
        // 게시물을 삭세하기 전에 content에 있는 S3 파일을 삭제
        if (!imageUrls.isEmpty()) {
            // S3에서 실제 파일 삭제
            imageUrls.forEach(imageUrl -> {
                try {
                    String filePath = extractFilePathFromUrl(imageUrl);
                    s3Service.deleteFile(filePath);
                    log.info("Deleted image from S3: {}", filePath);
                } catch (Exception e) {
                    log.error("Failed to delete image from S3: {}", imageUrl, e);
                }
            });
            log.info("Deleted {} images from S3 for post {}", imageUrls.size(), postId);
        }
    }

    // URL에서 파일 경로 추출 (예: https://bucket.s3.region.amazonaws.com/posts/filename.jpg -> posts/filename.jpg)
    // 이걸 해야 aws에서 삭제를 시킬 수 있다.
    private String extractFilePathFromUrl(String imageUrl) {
        String[] parts = imageUrl.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        throw new IllegalArgumentException("Invalid image URL format: " + imageUrl);
    }
}