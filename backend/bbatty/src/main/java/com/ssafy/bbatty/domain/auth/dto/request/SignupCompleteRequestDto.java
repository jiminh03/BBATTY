package com.ssafy.bbatty.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.ssafy.bbatty.domain.auth.constants.AuthConstants.NICKNAME_MAX_LENGTH;
import static com.ssafy.bbatty.domain.auth.constants.AuthConstants.NICKNAME_MIN_LENGTH;

@Getter
@NoArgsConstructor
public class SignupCompleteRequestDto {

    @NotNull(message = "팀 ID는 필수입니다.")
    @Positive(message = "팀 ID는 양수여야 합니다.")
    private Long teamId;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = NICKNAME_MIN_LENGTH, max = NICKNAME_MAX_LENGTH,
            message = "닉네임은 " + NICKNAME_MIN_LENGTH + "자 이상 " + NICKNAME_MAX_LENGTH + "자 이하여야 합니다.")
    private String nickname;

    @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    private String introduction;

    @NotBlank(message = "임시 토큰은 필수입니다.")
    private String stepToken;

    public SignupCompleteRequestDto(Long teamId, String nickname, String introduction, String stepToken) {
        this.teamId = teamId;
        this.nickname = nickname;
        this.introduction = introduction;
        this.stepToken = stepToken;
    }
}
