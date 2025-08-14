package com.ssafy.bbatty.domain.notification.service;

import com.ssafy.bbatty.domain.notification.dto.request.FCMTokenRequest;
import com.ssafy.bbatty.domain.notification.dto.response.NotificationSettingResponse;

public interface NotificationSettingService {

    NotificationSettingResponse registerFCMToken(Long userId, FCMTokenRequest request);
}