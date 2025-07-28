package com.ssafy.bbatty.domain.auth.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfoDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {

        @JsonProperty("email")
        private String email;

        @JsonProperty("name")
        private String name;

        @JsonProperty("birthyear")
        private String birthyear;

        @JsonProperty("birthday")
        private String birthday;

        @JsonProperty("gender")
        private String gender;
    }

    // 편의 메서드
    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.getEmail() : null;
    }

    public String getName() {
        return kakaoAccount != null ? kakaoAccount.getName() : null;
    }

    public String getBirthyear() {
        return kakaoAccount != null ? kakaoAccount.getBirthyear() : null;
    }

    public String getBirthday() {
        return kakaoAccount != null ? kakaoAccount.getBirthday() : null;
    }

    public String getGender() {
        return kakaoAccount != null ? kakaoAccount.getGender() : null;
    }

    public String getKakaoId() {
        return String.valueOf(id);
    }
}