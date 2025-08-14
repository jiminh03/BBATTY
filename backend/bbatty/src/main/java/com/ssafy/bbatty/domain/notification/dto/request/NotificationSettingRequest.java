package com.ssafy.bbatty.domain.notification.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationSettingRequest {

    private Boolean trafficSpikeAlertEnabled;
}