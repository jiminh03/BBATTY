package com.ssafy.bbatty.domain.notification.scheduler;

import com.ssafy.bbatty.domain.notification.entity.NotificationSetting;
import com.ssafy.bbatty.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FCMTokenCleanupScheduler {

    private final NotificationSettingRepository notificationSettingRepository;

    /**
     * 만료된 FCM 토큰 정리 (매일 새벽 3시)
     * 30일 이상 업데이트되지 않은 토큰들을 삭제
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            List<NotificationSetting> expiredSettings = notificationSettingRepository
                    .findExpiredTokens(cutoffDate);

            if (expiredSettings.isEmpty()) {
                log.info("정리할 만료된 FCM 토큰 없음");
                return;
            }

            notificationSettingRepository.deleteAll(expiredSettings);
            
            log.info("만료된 FCM 토큰 정리 완료 - 삭제된 토큰 수: {}", expiredSettings.size());

        } catch (Exception e) {
            log.error("FCM 토큰 정리 중 오류 발생", e);
        }
    }

    /**
     * 수동 테스트용 토큰 정리 (개발/테스트 환경에서만 사용)
     */
    public void manualCleanup() {
        log.info("수동 FCM 토큰 정리 시작");
        cleanupExpiredTokens();
    }

}