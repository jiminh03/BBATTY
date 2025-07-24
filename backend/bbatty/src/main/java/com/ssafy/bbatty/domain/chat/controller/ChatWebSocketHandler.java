package com.ssafy.bbatty.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler{
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.info("Websocket 연결 설정 : {}", session.getId());
    }
    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        log.info("메시지 수신: - 세션:{}, -내용:{}", session.getId(), message.getPayload());
        session.sendMessage(new TextMessage("서버응답: "+message.getPayload()));
    }
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("Websocket 연결 종료 : - 세션:{}, -내용:{}", session.getId(), status);
        // 사용자 퇴장 처리 해야함
    }
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류 - 세션 : {}", session.getId(), exception);
        // 오류 발생 시 연결 정리
        if (session.isOpen()) {
            session.close();
        }
    }
}
