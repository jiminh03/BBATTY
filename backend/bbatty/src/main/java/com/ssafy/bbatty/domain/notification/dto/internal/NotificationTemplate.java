package com.ssafy.bbatty.domain.notification.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationTemplate {
    
    private final String title;
    private final String body;
    private final String teamName;
    
    public static NotificationTemplate createTrafficSpikeAlert(String teamName) {
        return new NotificationTemplate(
                "🔥 " + teamName + " 직관 채팅 열기 폭발!",
                teamName + " 팬들이 열심히 응원하고 있어요! 지금 직관 인증하고 함께해보세요! ⚾",
                teamName
        );
    }
}