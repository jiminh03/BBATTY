package com.ssafy.bbatty.domain.auth.dto.response;

import com.ssafy.bbatty.domain.user.dto.response.UserDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupCompleteResponseDto {

    private final String message;
    private final TokenResponseDto tokens;
    private final UserDto user;

    public static SignupCompleteResponseDto success(TokenResponseDto tokens, UserDto user) {
        return SignupCompleteResponseDto.builder()
                .message("회원가입이 완료되었습니다.")
                .tokens(tokens)
                .user(user)
                .build();
    }
}