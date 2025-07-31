package com.ssafy.bbatty.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 * 카카오 로그인 후 추가 정보를 받아 회원가입 처리
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    // 카카오 액세스 토큰 (서버에서 검증용)
    @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
    private String accessToken;

    // 카카오에서 받은 정보들 (클라이언트가 전달)
    @NotBlank(message = "카카오 ID는 필수입니다.")
    private String kakaoId;

    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "생년은 필수입니다.")
    @Pattern(regexp = "^\\d{4}$", message = "생년은 4자리 숫자여야 합니다.")
    private String birthYear;

    @NotBlank(message = "성별은 필수입니다.")
    @Pattern(regexp = "^(male|female)$", message = "성별은 male 또는 female이어야 합니다.")
    private String gender;

    // 사용자가 입력한 추가 정보들
    @NotNull(message = "응원팀은 필수입니다.")
    private Long teamId;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
    private String nickname;

    @Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
    private String profileImg;

    @Size(max = 50, message = "자기소개는 50자 이하여야 합니다.")
    private String introduction;
}