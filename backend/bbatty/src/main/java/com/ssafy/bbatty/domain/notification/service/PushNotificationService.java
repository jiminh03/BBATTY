package com.ssafy.bbatty.domain.notification.service;

import com.ssafy.bbatty.domain.notification.entity.NotificationSetting;

import java.util.List;

public interface PushNotificationService {

    void sendTrafficSpikeAlert(Long teamId, String teamName);
    
    void sendBatchNotifications(List<NotificationSetting> targetUsers, String teamName);
}