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

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = NICKNAME_MIN_LENGTH, max = NICKNAME_MAX_LENGTH,
            message = "닉네임은 " + NICKNAME_MIN_LENGTH + "자 이상 " + NICKNAME_MAX_LENGTH + "자 이하여야 합니다.")
    private String nickname;

    @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    private String introduction;
    
    private String profileImg; // 선택사항

    // 카카오 정보
    @NotBlank(message = "카카오 ID는 필수입니다.")
    private String kakaoId;
    
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;
    
    @NotBlank(message = "이름은 필수입니다.")
    private String name;
    
    @NotBlank(message = "생년은 필수입니다.")
    private String birthyear;
    
    @NotBlank(message = "생일은 필수입니다.")
    private String birthday;
    
    @NotBlank(message = "성별은 필수입니다.")
    private String gender;

    // 팀 정보
    @NotNull(message = "팀 ID는 필수입니다.")
    @Positive(message = "팀 ID는 양수여야 합니다.")
    private Long teamId;
}
