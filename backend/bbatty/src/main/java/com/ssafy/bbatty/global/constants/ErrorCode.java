package com.ssafy.bbatty.global.constants;

import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    /*
    본인에게 맞는 예외코드 설정하세요
    // USER
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일이에요."),
	USER_DELETED(HttpStatus.NOT_FOUND, "탈퇴한 사용자예요."),
     */

    // COMMON
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없어요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없어요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요해요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청이에요."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했어요."),
    
    // FILE PATH
    INVALID_FILE_PATH(HttpStatus.BAD_REQUEST, "파일 확장자가 유효하지 않아요."),
    FILE_PATH_SECURITY_VIOLATION(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 경로예요."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일의 크기가 제한 크기를 초과합니다."),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "파일이 비어있습니다."),
    FILE_FAILED(HttpStatus.BAD_REQUEST, "파일 업로드에 실패했습니다."),

    // Auth 관련 에러코드
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."),
    INVALID_STEP_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 임시 토큰입니다."),
    DUPLICATE_SIGNUP(HttpStatus.CONFLICT, "이미 가입된 계정입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용중인 닉네임입니다."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 팀입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // POST
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없어요"),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없어요"),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "작성한 게시자만 삭제할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}