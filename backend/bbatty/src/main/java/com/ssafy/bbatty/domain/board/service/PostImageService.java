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
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
        "https://[^\\s/]+/posts/[a-f0-9-]+\\.(jpg|jpeg|png|gif|webp)",
        Pattern.CASE_INSENSITIVE
    );


    /*
    이미지 url을 받아서 DB에 넣는 과정이다.
    우선 초기에는 항상 null로 저장이된다.
     */
    @Transactional
    public PostImage saveUploadedImage(String imageUrl, Post post) {
        PostImage postImage = PostImage.builder()
                .post(post)
                .imageUrl(imageUrl)
                .status(PostImage.ImageStatus.UPLOADED)
                .build();
        
        return postImageRepository.save(postImage);
    }
    
    @Transactional
    public void processImagesInContent(String content, Post post) {
        List<String> imageUrls = extractImageUrls(content);
        
        if (!imageUrls.isEmpty()) {
            // 컨텐츠에 포함된 이미지들을 USED로 변경
            List<PostImage> uploadedImages = postImageRepository.findUploadedImagesByUrls(imageUrls);
            uploadedImages.forEach(PostImage::markAsUsed);
            postImageRepository.saveAll(uploadedImages);
            
            log.info("Marked {} images as USED for post {}", uploadedImages.size(), post.getId());
        }
        
        // 해당 포스트의 사용되지 않은 이미지들을 DELETED로 변경하고 S3에서 삭제
        markUnusedImagesAsDeleted(post);
    }
    
    @Transactional
    public void markUnusedImagesAsDeleted(Post post) {
        // post가 null인 경우(orphan images) 처리
        if (post == null) {
            return;
        }
        
        List<PostImage> postImages = postImageRepository.findByPostId(post.getId());
        
        List<PostImage> unusedImages = postImages.stream()
                .filter(PostImage::isOrphan)
                .collect(Collectors.toList());
        
        if (!unusedImages.isEmpty()) {
            // DB에서 DELETED로 마킹
            unusedImages.forEach(PostImage::markAsDeleted);
            postImageRepository.saveAll(unusedImages);
            
            // S3에서 실제 파일 삭제
            unusedImages.forEach(image -> {
                try {
                    String filePath = extractFilePathFromUrl(image.getImageUrl());
                    s3Service.deleteFile(filePath);
                    log.info("Deleted orphan image from S3: {}", filePath);
                } catch (Exception e) {
                    log.error("Failed to delete image from S3: {}", image.getImageUrl(), e);
                }
            });
            
            log.info("Marked {} unused images as DELETED for post {}", unusedImages.size(), post.getId());
        }
    }
    
    public List<String> extractImageUrls(String content) {
        return IMAGE_URL_PATTERN.matcher(content)
                .results()
                .map(matchResult -> matchResult.group())
                .collect(Collectors.toList());
    }
    
    private String extractFilePathFromUrl(String imageUrl) {
        // URL에서 파일 경로 추출 (예: https://bucket.s3.region.amazonaws.com/posts/filename.jpg -> posts/filename.jpg)
        String[] parts = imageUrl.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        throw new IllegalArgumentException("Invalid image URL format: " + imageUrl);
    }
    
    @Transactional
    public void cleanupOrphanImages() {
        List<PostImage> orphanImages = postImageRepository.findAllOrphanImages();
        
        if (!orphanImages.isEmpty()) {
            orphanImages.forEach(PostImage::markAsDeleted);
            postImageRepository.saveAll(orphanImages);
            
            // S3에서 실제 파일 삭제
            orphanImages.forEach(image -> {
                try {
                    String filePath = extractFilePathFromUrl(image.getImageUrl());
                    s3Service.deleteFile(filePath);
                    log.info("Cleaned up orphan image: {}", filePath);
                } catch (Exception e) {
                    log.error("Failed to cleanup orphan image: {}", image.getImageUrl(), e);
                }
            });
            
            log.info("Cleaned up {} orphan images", orphanImages.size());
        }
    }
}