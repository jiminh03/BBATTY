package com.ssafy.bbatty.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NicknameCheckResponseDto {

    private final boolean available;
    private final String message;

    public static NicknameCheckResponseDto available() {
        return NicknameCheckResponseDto.builder()
                .available(true)
                .message("사용 가능한 닉네임입니다.")
                .build();
    }

    public static NicknameCheckResponseDto unavailable() {
        return NicknameCheckResponseDto.builder()
                .available(false)
                .message("이미 사용 중인 닉네임입니다.")
                .build();
    }
}