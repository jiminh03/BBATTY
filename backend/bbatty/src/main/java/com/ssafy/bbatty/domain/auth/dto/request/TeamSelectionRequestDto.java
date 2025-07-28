package com.ssafy.bbatty.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamSelectionRequestDto {

    @NotNull(message = "팀 ID는 필수입니다.")
    @Positive(message = "팀 ID는 양수여야 합니다.")
    private Long teamId;

    public TeamSelectionRequestDto(Long teamId) {
        this.teamId = teamId;
    }
}