package com.ssafy.bbatty.domain.notification.dto.response;

import com.ssafy.bbatty.domain.notification.entity.NotificationSetting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationSettingResponse {

    private Long id;
    private String fcmToken;
    private String deviceId;
    private String deviceType;
    private Boolean trafficSpikeAlertEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.getId(),
                setting.getFcmToken(),
                setting.getDeviceId(),
                setting.getDeviceType(),
                setting.getTrafficSpikeAlertEnabled(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }
}