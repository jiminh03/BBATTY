package com.ssafy.bbatty.global.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 1. 조회용 S3 URL 생성 (public-read라면 이 URL 그대로 사용 가능)
    public String getPublicUrl(String filePath) {
        return amazonS3.getUrl(bucket, filePath).toString();
    }

    // 2. 서버에서 직접 파일 업로드
    public String uploadFile(MultipartFile file, String directory) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename());
        String filePath = directory + "/" + fileName;
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        
        PutObjectRequest request = new PutObjectRequest(bucket, filePath, file.getInputStream(), metadata);
        amazonS3.putObject(request);
        
        return filePath;
    }

    /**
     * 파일명 중복을 방지하기 위해 UUID를 사용해 고유한 파일명을 생성합니다.
     * 원본 파일의 확장자는 유지합니다.
     *
     * 예: "photo.jpg" → "550e8400-e29b-41d4-a716-446655440000.jpg"
     *
     * @param originalFilename 원본 파일명
     * @return 고유한 UUID 기반 파일명
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    // 3. 프론트엔드를 위한 Presigned URL 생성 (업로드용)
    public String generatePresignedUploadUrl(String directory, String originalFilename) {
        String fileName = generateFileName(originalFilename);
        String filePath = directory + "/" + fileName;
        
        // 15분 후 만료되는 presigned URL 생성
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 15; // 15분
        expiration.setTime(expTimeMillis);
        
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, filePath)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);
        
        // Content-Type 헤더를 설정하도록 요구
        generatePresignedUrlRequest.addRequestParameter("Content-Type", "image/*");
        
        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }
    
    // 4. 파일 경로와 presigned URL을 함께 반환하는 메서드
    public PresignedUrlResponse generatePresignedUploadUrlWithPath(String directory, String originalFilename) {
        // 이미지 파일 확장자 검증
        if (!isValidImageFile(originalFilename)) {
            throw new IllegalArgumentException("Invalid image file format. Only jpg, jpeg, png, gif, webp are allowed.");
        }
        
        String fileName = generateFileName(originalFilename);
        String filePath = directory + "/" + fileName;
        
        // 15분 후 만료되는 presigned URL 생성
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 15; // 15분
        expiration.setTime(expTimeMillis);
        
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, filePath)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);
        
        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        String publicUrl = getPublicUrl(filePath);
        
        return new PresignedUrlResponse(url.toString(), publicUrl, filePath);
    }

    // 5. S3 파일 삭제 (게시물 삭제 시 호출)
    public void deleteFile(String filePath) {
        // 파일 경로 유효성 검증 메서드
        amazonS3.deleteObject(bucket, filePath);
    }
    
    /**
     * 이미지 파일 확장자 검증
     */
    private boolean isValidImageFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String lowercaseFilename = filename.toLowerCase();
        return lowercaseFilename.endsWith(".jpg") || 
               lowercaseFilename.endsWith(".jpeg") || 
               lowercaseFilename.endsWith(".png") || 
               lowercaseFilename.endsWith(".gif") || 
               lowercaseFilename.endsWith(".webp");
    }
    
    // Presigned URL 응답을 위한 내부 클래스
    public static class PresignedUrlResponse {
        private final String uploadUrl;
        private final String fileUrl;
        private final String filePath;
        
        public PresignedUrlResponse(String uploadUrl, String fileUrl, String filePath) {
            this.uploadUrl = uploadUrl;
            this.fileUrl = fileUrl;
            this.filePath = filePath;
        }
        
        public String getUploadUrl() { return uploadUrl; }
        public String getFileUrl() { return fileUrl; }
        public String getFilePath() { return filePath; }
    }
}