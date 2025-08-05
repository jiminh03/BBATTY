package com.ssafy.bbatty.global.exception;


import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.response.ApiResponse;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ErrorCode errorCode = mapToErrorCode(e);
        log.error("Spring error occurred: {} - {}", errorCode.getMessage(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<?>> handleApiException(ApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("API error occurred: {}", errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception e) {
        ErrorCode errorCode = ErrorCode.SERVER_ERROR;
        log.error("General error occurred: {} - {}", errorCode.getMessage(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

    private ErrorCode mapToErrorCode(Exception e) {
        if (e instanceof MethodArgumentNotValidException
                || e instanceof HttpMessageNotReadableException
                || e instanceof HandlerMethodValidationException
                || e instanceof MissingServletRequestParameterException
                || e instanceof HttpRequestMethodNotSupportedException
                || e instanceof HttpMediaTypeNotSupportedException
                || e instanceof TypeMismatchException
                || e instanceof ServletRequestBindingException
                || e instanceof MissingPathVariableException
                || e instanceof HttpMediaTypeNotAcceptableException) {
            return ErrorCode.BAD_REQUEST;
        }

        if (e instanceof NoHandlerFoundException
                || e instanceof NoResourceFoundException) {
            return ErrorCode.NOT_FOUND;
        }

        return ErrorCode.SERVER_ERROR;
    }

}