package com.ssafy.chat.global.constants;

import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // COMMON
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없어요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없어요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요해요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청이에요."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했어요."),

    // MATCH CHAT
    MATCH_CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 채팅방을 찾을 수 없어요."),
    MATCH_CHAT_ROOM_FULL(HttpStatus.CONFLICT, "매칭 채팅방이 가득 찼어요."),
    MATCH_CHAT_ROOM_CLOSED(HttpStatus.CONFLICT, "닫힌 매칭 채팅방이에요."),
    INVALID_MATCH_CONDITIONS(HttpStatus.BAD_REQUEST, "잘못된 매칭 조건이에요."),
    DUPLICATE_MATCH_CHAT_ROOM(HttpStatus.CONFLICT, "이미 존재하는 매칭 채팅방이에요."),

    // WEBSOCKET
    WEBSOCKET_CONNECTION_FAILED(HttpStatus.BAD_REQUEST, "웹소켓 연결에 실패했어요."),
    INVALID_SESSION_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 세션 토큰이에요."),

    // KAFKA
    KAFKA_MESSAGE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 전송에 실패했어요."),
    KAFKA_MESSAGE_CONSUME_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 수신에 실패했어요.");

    private final HttpStatus status;
    private final String message;
}