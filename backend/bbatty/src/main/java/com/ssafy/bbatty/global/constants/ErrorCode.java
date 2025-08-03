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
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일의 크기가 제한 크기를 초과해요."),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "파일이 비어있어요."),
    FILE_FAILED(HttpStatus.BAD_REQUEST, "파일 업로드에 실패했어요."),

    // Auth 관련 에러코드
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했어요."),
    INVALID_STEP_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 임시 토큰이에요."),
    DUPLICATE_SIGNUP(HttpStatus.CONFLICT, "이미 가입된 계정이에요."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용중인 닉네임이에요."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 팀이에요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰이에요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자에요."),
    SHA_256_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "SHA-256 알고리즘을 찾을 수 없어요."),
    REFRESH_TOKEN_MISSING(HttpStatus.NOT_FOUND, "존재하지 않는 리프레쉬 토큰이에요."),

    // Redis 관련 에러코드
    REDIS_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Redis 서버에 연결할 수 없어요."),
    REDIS_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 작업 처리 중 오류가 발생했어요."),

    // 카카오 정보 관련 에러
    KAKAO_EMAIL_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "카카오에서 이메일 정보를 가져올 수 없어요."),
    KAKAO_BIRTH_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "카카오에서 생년 정보를 가져올 수 없어요."),
    KAKAO_BIRTH_INFO_INVALID(HttpStatus.BAD_REQUEST, "카카오 생년 정보가 올바르지 않아요."),
    KAKAO_GENDER_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "카카오에서 성별 정보를 가져올 수 없어요."),
    KAKAO_GENDER_INFO_INVALID(HttpStatus.BAD_REQUEST, "카카오 성별 정보가 올바르지 않아요."),

    // POST
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없어요"),
    FORBIDDEN_TEAM(HttpStatus.FORBIDDEN, "해당 팀만 사용가능한 기능입니다."),

    // COMMENT
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없어요"),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "작성한 게시자만 삭제할 수 있어요.");

    private final HttpStatus status;
    private final String message;
}