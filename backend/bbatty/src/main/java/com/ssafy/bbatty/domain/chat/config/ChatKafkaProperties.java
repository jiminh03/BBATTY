package com.ssafy.bbatty.domain.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 채팅 관련 Kafka 설정 프로퍼티
 */
@Component
@ConfigurationProperties(prefix = "chat.kafka")
@Getter
@Setter
public class ChatKafkaProperties {
    
    /**
     * 토픽 설정
     */
    private Topics topics = new Topics();
    
    /**
     * 그룹 ID 설정
     */
    private Groups groups = new Groups();
    
    @Getter
    @Setter
    public static class Topics {
        private String authResult = "chat-auth-result";
        private String matchChatRequest = "match-chat-request";
        private String watchChatRequest = "watch-chat-request";
    }
    
    @Getter
    @Setter
    public static class Groups {
        private String matchChatGroup = "bbatty-match-chat-group";
        private String watchChatGroup = "bbatty-watch-chat-group";
    }
}