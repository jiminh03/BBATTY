package com.ssafy.bbatty.global.dto.response;


import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Status;
import com.ssafy.bbatty.global.constants.SuccessCode;

public record ApiResponse<T>(
        Status status,
        Enum<?> code,
        T data
) {
    public static <T> ApiResponse<T> success(SuccessCode successCode, T data) {
        return new ApiResponse<>(Status.SUCCESS, successCode, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Status.SUCCESS, SuccessCode.SUCCESS_DEFAULT, data);
    }

    public static ApiResponse<Void> success(SuccessCode successCode) {
        return new ApiResponse<>(Status.SUCCESS, successCode, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(Status.SUCCESS, SuccessCode.SUCCESS_DEFAULT, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>(Status.ERROR, errorCode, data);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(Status.ERROR, errorCode, null);
    }
}