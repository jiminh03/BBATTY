package com.ssafy.chat.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatRoomCreateRequest {
    @Min(value = 1, message = "유효한 경기 ID를 입력해주세요.")
    private Long gameId;

    @NotBlank(message = "매칭방 제목은 필수입니다.")
    @Size(max = 20, message = "매칭 제목은 20자를 초과할 수 없습니다.")
    private String matchTitle;

    @Size(max=50, message = "매칭 설명은 50자를 초과할 수 없습니다.")
    private String matchDescription;

    // 팀 조건
    @Min(value = 1, message = "유효한 팀 ID를 입력해주세요.")
    private Long teamId;

    // 나이
    @Min(value = 10, message = "최소 나이는 20세 입니다. (미성년자 불가)")
    @Max(value = 100, message = "최대 나이는 100세입니다.")
    private int minAge;

    @Min(value = 10, message = "최소 나이는 20세 입니다. (미성년자 불가)")
    @Max(value = 100, message = "최대 나이는 100세입니다.")
    private int maxAge;

    // 성별 조건 (MALE, FEMALE, ALL)
    @NotBlank(message = "성별 조건은 필수입니다.")
    private String genderCondition;

    // 최대 참여 인원 (기본값: )
    @Min(value = 2, message = "최소 2명 이상이어야 합니다.")
    @Max(value = 16, message = "최대 16명 이하여야 합니다.")
    private int maxParticipants;

}
