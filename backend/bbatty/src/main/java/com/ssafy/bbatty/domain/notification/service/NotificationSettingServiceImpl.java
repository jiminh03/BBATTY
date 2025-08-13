package com.ssafy.bbatty.domain.notification.service;

import com.ssafy.bbatty.domain.notification.dto.request.FCMTokenRequest;
import com.ssafy.bbatty.domain.notification.dto.request.NotificationSettingRequest;
import com.ssafy.bbatty.domain.notification.dto.response.NotificationSettingResponse;
import com.ssafy.bbatty.domain.notification.entity.NotificationSetting;
import com.ssafy.bbatty.domain.notification.repository.NotificationSettingRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    @Override
    public NotificationSettingResponse registerFCMToken(Long userId, FCMTokenRequest request) {
        User user = findUserById(userId);

        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .map(existing -> {
                    existing.updateFcmToken(request.getFcmToken(), request.getDeviceId(), request.getDeviceType());
                    return existing;
                })
                .orElseGet(() -> {
                    NotificationSetting newSetting = NotificationSetting.createWithFcmToken(
                            user, request.getFcmToken(), request.getDeviceId(), request.getDeviceType()
                    );
                    return notificationSettingRepository.save(newSetting);
                });

        log.info("FCM 토큰 등록/업데이트 완료 - userId: {}, deviceType: {}", userId, request.getDeviceType());
        return NotificationSettingResponse.from(setting);
    }

    @Override
    public NotificationSettingResponse updateNotificationSettings(Long userId, NotificationSettingRequest request) {
        NotificationSetting setting = findNotificationSettingByUserId(userId);
        
        setting.updateNotificationSettings(request.getTrafficSpikeAlertEnabled());
        
        log.info("알림 설정 업데이트 완료 - userId: {}, trafficSpikeAlertEnabled: {}", 
                userId, request.getTrafficSpikeAlertEnabled());
        
        return NotificationSettingResponse.from(setting);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSettingResponse getNotificationSettings(Long userId) {
        NotificationSetting setting = findNotificationSettingByUserId(userId);
        return NotificationSettingResponse.from(setting);
    }

    @Override
    public void deleteFCMToken(Long userId) {
        notificationSettingRepository.deleteByUserId(userId);
        log.info("FCM 토큰 삭제 완료 - userId: {}", userId);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private NotificationSetting findNotificationSettingByUserId(Long userId) {
        return notificationSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));
    }
}