//package com.ssafy.bbatty.global.s3;
//
//
//import com.amazonaws.HttpMethod;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
//import com.ssafy.bbatty.global.util.FilePathValidator;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.net.URL;
//import java.util.Date;
//
//@Service
//@RequiredArgsConstructor
//public class S3Service {
//
//    private final AmazonS3 amazonS3;
//
//    @Value("${cloud.aws.s3.bucket}")
//    private String bucket;
//
//    // 1. Presigned PUT URL 발급 (React Native에서 이걸로 직접 업로드)
//    public String generatePresignedPutUrl(String filePath, int expireMinutes) {
//        // 파일 경로 유효성 검증 메서드
//        FilePathValidator.validateImagePath(filePath);
//
//        Date expiration = new Date(System.currentTimeMillis() + 1000L * 60 * expireMinutes);
//        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, filePath)
//                .withMethod(HttpMethod.PUT)
//                .withExpiration(expiration);
//        URL presignedUrl = amazonS3.generatePresignedUrl(request);
//        return presignedUrl.toString();
//    }
//
//    // 2. 조회용 S3 URL 생성 (public-read라면 이 URL 그대로 사용 가능)
//    public String getPublicUrl(String filePath) {
//        // 파일 경로 유효성 검증 메서드
//        FilePathValidator.validateImagePath(filePath);
//        return amazonS3.getUrl(bucket, filePath).toString();
//    }
//
//    // 3. S3 파일 삭제 (게시물 삭제 시 호출)
//    public void deleteFile(String filePath) {
//        // 파일 경로 유효성 검증 메서드
//        FilePathValidator.validateImagePath(filePath);
//        amazonS3.deleteObject(bucket, filePath);
//    }
//}