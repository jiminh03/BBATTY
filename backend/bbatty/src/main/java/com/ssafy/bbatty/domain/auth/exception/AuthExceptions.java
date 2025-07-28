package com.ssafy.bbatty.domain.auth.exception;

import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;

/**
 * 인증 관련 예외 클래스들
 */
public class AuthExceptions {

    /**
     * 카카오 인증 실패 예외
     */
    public static class KakaoAuthException extends ApiException {
        public KakaoAuthException() {
            super(ErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    /**
     * Step Token 관련 예외
     */
    public static class InvalidStepTokenException extends ApiException {
        public InvalidStepTokenException() {
            super(ErrorCode.INVALID_STEP_TOKEN);
        }
    }

    /**
     * 중복 회원가입 예외
     */
    public static class DuplicateSignupException extends ApiException {
        public DuplicateSignupException() {
            super(ErrorCode.DUPLICATE_SIGNUP);
        }
    }

    /**
     * 닉네임 중복 예외
     */
    public static class DuplicateNicknameException extends ApiException {
        public DuplicateNicknameException() {
            super(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    /**
     * 팀 존재하지 않음 예외
     */
    public static class TeamNotFoundException extends ApiException {
        public TeamNotFoundException() {
            super(ErrorCode.TEAM_NOT_FOUND);
        }
    }

    /**
     * JWT 토큰 관련 예외
     */
    public static class InvalidTokenException extends ApiException {
        public InvalidTokenException() {
            super(ErrorCode.INVALID_TOKEN);
        }
        
        public InvalidTokenException(String message) {
            super(ErrorCode.INVALID_TOKEN, message);
        }
    }

    /**
     * 사용자 존재하지 않음 예외
     */
    public static class UserNotFoundException extends ApiException {
        public UserNotFoundException() {
            super(ErrorCode.USER_NOT_FOUND);
        }
    }
}