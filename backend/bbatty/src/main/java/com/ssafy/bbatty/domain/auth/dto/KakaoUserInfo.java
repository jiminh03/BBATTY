package com.ssafy.bbatty.domain.auth.dto;

import com.ssafy.bbatty.global.constants.Gender;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoUserInfo {
    private final String kakaoId;
    private final String email;
    private final Gender gender;
    private final Integer birthYear;
}