package com.ssafy.bbatty.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 사용자 정보 API 응답 DTO
 * 필요한 정보만 추출: 이메일, 생년(연도만), 성별
 */
@Getter
@NoArgsConstructor
public class KakaoUserResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {

        @JsonProperty("email")
        private String email;

        @JsonProperty("is_email_valid")
        private Boolean isEmailValid;

        @JsonProperty("is_email_verified")
        private Boolean isEmailVerified;

        @JsonProperty("birthyear")
        private String birthyear;

        @JsonProperty("gender")
        private String gender;
    }

    // 비즈니스 로직 메서드들

    /**
     * 카카오 사용자 ID 반환
     */
    public String getKakaoId() {
        return this.id;
    }

    /**
     * 이메일 반환 (검증된 이메일만)
     */
    public String getEmail() {
        if (kakaoAccount != null &&
                Boolean.TRUE.equals(kakaoAccount.isEmailValid) &&
                Boolean.TRUE.equals(kakaoAccount.isEmailVerified)) {
            return kakaoAccount.email;
        }
        return null;
    }

    /**
     * 생년 반환 (YYYY 형태)
     */
    public String getBirthYear() {
        return kakaoAccount != null ? kakaoAccount.birthyear : null;
    }

    /**
     * 성별 반환 (male/female)
     */
    public String getGender() {
        return kakaoAccount != null ? kakaoAccount.gender : null;
    }
}
