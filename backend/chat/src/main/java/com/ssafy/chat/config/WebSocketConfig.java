package com.ssafy.chat.config;

import com.ssafy.chat.common.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 설정
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("WebSocket 핸들러 등록 시작");

        // 관전 채팅 WebSocket 엔드포인트
        registry.addHandler(chatWebSocketHandler, "/ws/watch-chat")
                .addInterceptors(webSocketHandshakeInterceptor) // 핸드셰이크 인터셉터 추가
                .setAllowedOriginPatterns("*") // 인증정보 + 모든 사이트 요청 가능
                .withSockJS(); // SockJS 지원

        // 매칭 채팅 WebSocket 엔드포인트 (순수 WebSocket - React Native 지원)
        registry.addHandler(chatWebSocketHandler, "/ws/match-chat")
                .addInterceptors(webSocketHandshakeInterceptor) // 핸드셰이크 인터셉터 추가
                .setAllowedOriginPatterns("*"); // 인증정보 + 모든 사이트 요청 가능

        log.info("WebSocket 핸들러 등록 완료 - /ws/watch-chat, /ws/match-chat");
    }
}