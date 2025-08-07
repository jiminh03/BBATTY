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
    KAFKA_MESSAGE_CONSUME_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 수신에 실패했어요."),
    
    // GAME
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "경기 정보를 찾을 수 없어요."),
    GAME_FINISHED(HttpStatus.BAD_REQUEST, "이미 종료된 경기예요."),
    GAME_NOT_LIVE(HttpStatus.BAD_REQUEST, "라이브 중인 경기가 아니에요."),
    
    // TEAM
    UNAUTHORIZED_TEAM_ACCESS(HttpStatus.FORBIDDEN, "해당 팀에 대한 권한이 없어요."),
    TEAM_NOT_IN_GAME(HttpStatus.BAD_REQUEST, "해당 팀이 경기에 참여하지 않아요."),
    
    // WATCH CHAT
    WATCH_CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "관전 채팅방을 찾을 수 없어요."),
    WATCH_CHAT_ROOM_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "관전 채팅방 생성에 실패했어요."),
    WATCH_CHAT_ROOM_LIST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "관전 채팅방 목록 조회에 실패했어요."),
    WATCH_CHAT_ROOM_INFO_INVALID(HttpStatus.BAD_REQUEST, "관전 채팅방 정보가 올바르지 않아요."),
    
    // REDIS
    REDIS_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Redis 연결에 실패했어요."),
    REDIS_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 작업에 실패했어요."),
    REDIS_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "Redis에서 키를 찾을 수 없어요."),
    REDIS_DATA_PARSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 데이터 파싱에 실패했어요."),
    
    // VALIDATION
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않아요."),
    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "필수 필드가 누락되었어요."),
    DATA_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "데이터 형식이 올바르지 않아요."),
    
    // MATCH ID VALIDATION
    MATCH_ID_MISSING(HttpStatus.BAD_REQUEST, "매칭 ID가 누락되었어요."),
    MATCH_ID_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "매칭 ID 형식이 올바르지 않아요."),
    MATCH_ID_PREFIX_INVALID(HttpStatus.BAD_REQUEST, "매칭 ID는 'match_'로 시작해야 해요."),
    MATCH_ID_GAME_ID_INVALID(HttpStatus.BAD_REQUEST, "매칭 ID에서 경기 ID를 추출할 수 없어요.");
    private final HttpStatus status;
    private final String message;
}