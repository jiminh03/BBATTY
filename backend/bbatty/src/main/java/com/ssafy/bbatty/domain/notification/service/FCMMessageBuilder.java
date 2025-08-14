package com.ssafy.bbatty.domain.notification.service;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.ssafy.bbatty.domain.notification.dto.internal.NotificationTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FCMMessageBuilder {

    public MulticastMessage buildTrafficSpikeAlert(List<String> fcmTokens, NotificationTemplate template) {
        Notification notification = Notification.builder()
                .setTitle(template.getTitle())
                .setBody(template.getBody())
                .build();

        Map<String, String> data = Map.of(
                "type", "TRAFFIC_SPIKE_ALERT",
                "teamName", template.getTeamName(),
                "timestamp", String.valueOf(System.currentTimeMillis())
        );

        return MulticastMessage.builder()
                .setNotification(notification)
                .putAllData(data)
                .addAllTokens(fcmTokens)
                .build();
    }

    public Message buildSingleMessage(String fcmToken, NotificationTemplate template) {
        Notification notification = Notification.builder()
                .setTitle(template.getTitle())
                .setBody(template.getBody())
                .build();

        Map<String, String> data = Map.of(
                "type", "TRAFFIC_SPIKE_ALERT",
                "teamName", template.getTeamName(),
                "timestamp", String.valueOf(System.currentTimeMillis())
        );

        return Message.builder()
                .setNotification(notification)
                .putAllData(data)
                .setToken(fcmToken)
                .build();
    }
}