package com.ssafy.bbatty.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequestDto {

    @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
    private String accessToken;

    public KakaoLoginRequestDto(String accessToken) {
        this.accessToken = accessToken;
    }
}
