package com.ssafy.bbatty.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupCompleteResponseDto {

    private final String message;
    private final TokenResponseDto tokens;
    private final UserInfoDto userInfo;

    @Getter
    @Builder
    public static class UserInfoDto {
        private final Long userId;
        private final String nickname;
        private final String teamName;
        private final String profileImg;
    }

    public static SignupCompleteResponseDto success(TokenResponseDto tokens, UserInfoDto userInfo) {
        return SignupCompleteResponseDto.builder()
                .message("회원가입이 완료되었습니다.")
                .tokens(tokens)
                .userInfo(userInfo)
                .build();
    }
}