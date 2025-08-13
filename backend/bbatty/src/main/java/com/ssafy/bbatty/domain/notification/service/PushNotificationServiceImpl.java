package com.ssafy.bbatty.domain.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.ssafy.bbatty.domain.notification.dto.internal.NotificationTemplate;
import com.ssafy.bbatty.domain.notification.entity.NotificationSetting;
import com.ssafy.bbatty.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final FCMMessageBuilder fcmMessageBuilder;
    private final FirebaseMessaging firebaseMessaging;

    @Value("${notification.fcm.batch-size:500}")
    private Integer batchSize;

    @Override
    @Transactional
    public void sendTrafficSpikeAlert(Long teamId, String teamName) {
        List<NotificationSetting> targetUsers = notificationSettingRepository
                .findTrafficSpikeAlertEnabledUsers(teamId);

        if (targetUsers.isEmpty()) {
            log.info("트래픽 급증 알림 대상 사용자 없음 - teamId: {}", teamId);
            return;
        }

        log.info("트래픽 급증 알림 발송 시작 - teamId: {}, targetCount: {}", teamId, targetUsers.size());
        sendBatchNotifications(targetUsers, teamName);
    }

    @Override
    @Transactional
    public void sendBatchNotifications(List<NotificationSetting> targetUsers, String teamName) {
        NotificationTemplate template = NotificationTemplate.createTrafficSpikeAlert(teamName);

        IntStream.range(0, (targetUsers.size() + batchSize - 1) / batchSize)
                .parallel()
                .forEach(batchIndex -> {
                    int start = batchIndex * batchSize;
                    int end = Math.min(start + batchSize, targetUsers.size());
                    List<NotificationSetting> batch = targetUsers.subList(start, end);

                    sendBatch(batch, template, batchIndex + 1);
                });

        log.info("트래픽 급증 알림 발송 완료 - teamName: {}, totalCount: {}", teamName, targetUsers.size());
    }

    private void sendBatch(List<NotificationSetting> batch, NotificationTemplate template, int batchNumber) {
        try {
            List<String> fcmTokens = batch.stream()
                    .map(NotificationSetting::getFcmToken)
                    .toList();

            MulticastMessage message = fcmMessageBuilder.buildTrafficSpikeAlert(fcmTokens, template);
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

            log.info("배치 알림 발송 완료 - batch: {}, success: {}, failure: {}", 
                    batchNumber, response.getSuccessCount(), response.getFailureCount());

        } catch (FirebaseMessagingException e) {
            log.error("배치 알림 발송 실패 - batch: {}, error: {}", batchNumber, e.getMessage());
        } catch (Exception e) {
            log.error("배치 처리 중 예외 발생 - batch: {}", batchNumber, e);
        }
    }

}