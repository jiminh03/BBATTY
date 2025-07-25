package com.ssafy.bbatty.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoLoginResponseDto {

    private final boolean isExistingUser;
    private final String stepToken; // 신규 사용자용
    private final TokenResponseDto tokens; // 기존 사용자용

    public static KakaoLoginResponseDto forNewUser(String stepToken) {
        return KakaoLoginResponseDto.builder()
                .isExistingUser(false)
                .stepToken(stepToken)
                .build();
    }

    public static KakaoLoginResponseDto forExistingUser(TokenResponseDto tokens) {
        return KakaoLoginResponseDto.builder()
                .isExistingUser(true)
                .tokens(tokens)
                .build();
    }
}