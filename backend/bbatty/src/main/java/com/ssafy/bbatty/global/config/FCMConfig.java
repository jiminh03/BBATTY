package com.ssafy.bbatty.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Cloud Messaging 설정 클래스
 * Firebase Admin SDK를 초기화하고 FCM 서비스를 구성합니다.
 */
@Configuration
@Slf4j
public class FCMConfig {

    @Value("${firebase.credentials.path}")
    private String firebaseCredentialsPath;

    @Value("${firebase.project-id}")
    private String firebaseProjectId;

    @Value("${notification.fcm.enabled:true}")
    private boolean fcmEnabled;

    /**
     * Firebase App 초기화
     * 애플리케이션 시작 시 Firebase Admin SDK를 초기화합니다.
     */
    @PostConstruct
    public void initialize() {
        if (!fcmEnabled) {
            log.info("FCM이 비활성화되어 있습니다. FCM 초기화를 건너뜁니다.");
            return;
        }

        try {
            // 이미 초기화된 경우 건너뛰기
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase App이 이미 초기화되어 있습니다.");
                return;
            }

            InputStream serviceAccount = getFirebaseCredentials();
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(firebaseProjectId)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase App 초기화 완료 - Project ID: {}", firebaseProjectId);

        } catch (IOException e) {
            log.error("Firebase 초기화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase 초기화에 실패했습니다.", e);
        }
    }

    /**
     * Firebase Messaging Bean 생성
     * FCM 메시지 전송을 위한 FirebaseMessaging 인스턴스를 제공합니다.
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!fcmEnabled) {
            log.warn("FCM이 비활성화되어 있습니다. FirebaseMessaging Bean을 생성하지 않습니다.");
            return null;
        }

        try {
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.error("FirebaseMessaging 인스턴스 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("FirebaseMessaging 인스턴스 생성에 실패했습니다.", e);
        }
    }

    /**
     * Firebase 인증 정보 로드
     * 환경에 따라 파일 경로 또는 클래스패스에서 인증 정보를 로드합니다.
     */
    private InputStream getFirebaseCredentials() throws IOException {
        try {
            // 절대 경로 또는 상대 경로로 파일 로드 시도
            return new FileInputStream(firebaseCredentialsPath);
        } catch (IOException e) {
            log.warn("파일 경로에서 Firebase 인증 정보를 찾을 수 없습니다. 클래스패스에서 시도합니다: {}", firebaseCredentialsPath);
            
            // 클래스패스에서 로드 시도
            try {
                ClassPathResource resource = new ClassPathResource(firebaseCredentialsPath);
                return resource.getInputStream();
            } catch (IOException ex) {
                log.error("클래스패스에서도 Firebase 인증 정보를 찾을 수 없습니다: {}", firebaseCredentialsPath);
                throw new IOException("Firebase 인증 정보를 찾을 수 없습니다: " + firebaseCredentialsPath, ex);
            }
        }
    }

    /**
     * FCM 활성화 상태 확인
     */
    public boolean isFcmEnabled() {
        return fcmEnabled;
    }
}