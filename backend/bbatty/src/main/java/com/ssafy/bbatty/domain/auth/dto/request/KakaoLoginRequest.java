package com.ssafy.bbatty.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 로그인 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginRequest {

    @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
    private String accessToken;
}