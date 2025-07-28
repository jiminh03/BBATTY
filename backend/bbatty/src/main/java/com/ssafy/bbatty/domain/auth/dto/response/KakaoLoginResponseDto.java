package com.ssafy.bbatty.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoLoginResponseDto {

    private final boolean isNewUser;
    private final TokenResponseDto tokens;
    private final Object user;
    
    // 신규 사용자인 경우 카카오 정보
    private final String kakaoId;
    private final String email;
    private final String name;
    private final String birthyear;
    private final String birthday;
    private final String gender;

    public static KakaoLoginResponseDto forNewUser(String kakaoId, String email, String name, 
                                                  String birthyear, String birthday, String gender) {
        return KakaoLoginResponseDto.builder()
                .isNewUser(true)
                .kakaoId(kakaoId)
                .email(email)
                .name(name)
                .birthyear(birthyear)
                .birthday(birthday)
                .gender(gender)
                .build();
    }

    public static KakaoLoginResponseDto forExistingUser(TokenResponseDto tokens, Object user) {
        return KakaoLoginResponseDto.builder()
                .isNewUser(false)
                .tokens(tokens)
                .user(user)
                .build();
    }
}