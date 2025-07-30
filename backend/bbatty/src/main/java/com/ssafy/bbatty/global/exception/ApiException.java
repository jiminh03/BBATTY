package com.ssafy.bbatty.global.exception;


import com.ssafy.bbatty.global.constants.ErrorCode;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private ErrorCode errorCode;

    /*
    해당 메서드를 통해서 예외를 발생시키시면 됩니다.
    super는 백엔드 개발자를 위한 로그 메시지,
    errorCode는 프론트(클라이언트)를 위한 응답 메시지입니다.
     */
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}