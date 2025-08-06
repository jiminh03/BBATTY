package com.ssafy.chat.global.constants;

import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    SUCCESS_DEFAULT(HttpStatus.OK, "요청을 성공적으로 처리했어요."),
    SUCCESS_CREATED(HttpStatus.CREATED, "리소스가 성공적으로 생성됐어요."),
    SUCCESS_UPDATED(HttpStatus.OK, "리소스가 성공적으로 업데이트됐어요."),
    SUCCESS_DELETED(HttpStatus.OK, "리소스가 성공적으로 삭제됐어요.");

    private final HttpStatus status;
    private final String message;
}