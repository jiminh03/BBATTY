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

    // 4. S3 파일 삭제 (게시물 삭제 시 호출)
    public void deleteFile(String filePath) {
        // 파일 경로 유효성 검증 메서드
        amazonS3.deleteObject(bucket, filePath);
    }
}