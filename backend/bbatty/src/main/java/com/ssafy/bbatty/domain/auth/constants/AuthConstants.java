package com.ssafy.bbatty.domain.auth.constants;

public final class AuthConstants {

    private AuthConstants() {}

    // JWT 관련
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final String JWT_TYPE_ACCESS = "access";
    public static final String JWT_TYPE_REFRESH = "refresh";

    // 카카오 OAuth 관련
    public static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    public static final String KAKAO_TOKEN_PREFIX = "Bearer ";

    // Redis 관련
    public static final String REDIS_BLACKLIST_PREFIX = "blacklist:";
    public static final String REDIS_REFRESH_TOKEN_PREFIX = "refresh:";
    public static final String REDIS_SIGNUP_TEMP_PREFIX = "signup:";

    // 회원가입 관련
    public static final int NICKNAME_MIN_LENGTH = 2;
    public static final int NICKNAME_MAX_LENGTH = 20;
    public static final long SIGNUP_TEMP_EXPIRE_MINUTES = 30;

    // 응답 메시지
    public static final String LOGIN_SUCCESS_MESSAGE = "로그인이 완료되었습니다.";
    public static final String SIGNUP_SUCCESS_MESSAGE = "회원가입이 완료되었습니다.";
    public static final String LOGOUT_SUCCESS_MESSAGE = "로그아웃이 완료되었습니다.";
    public static final String TOKEN_REFRESH_SUCCESS_MESSAGE = "토큰이 갱신되었습니다.";
    public static final String NICKNAME_AVAILABLE_MESSAGE = "사용 가능한 닉네임입니다.";
}