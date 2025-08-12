package com.ssafy.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Chat 관련 설정값들을 관리하는 Properties 클래스
 */
@Component
@ConfigurationProperties(prefix = "chat")
@Getter
@Setter
public class ChatProperties {

    private final Websocket websocket = new Websocket();
    private final Traffic traffic = new Traffic();
    private final Auth auth = new Auth();

    @Getter
    @Setter
    public static class Websocket {
        private String baseUrl = "ws://i13a403.p.ssafy.io:8084";
    }

    @Getter
    @Setter
    public static class Traffic {
        private int spikeThreshold = 50;
        private int windowMinutes = 1;
    }

    @Getter
    @Setter
    public static class Auth {
        private long timeoutMs = 10000L;
    }
}