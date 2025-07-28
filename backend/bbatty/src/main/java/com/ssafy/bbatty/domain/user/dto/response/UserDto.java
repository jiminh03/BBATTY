package com.ssafy.bbatty.domain.user.dto.response;

import com.ssafy.bbatty.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDto {
    private final Long userId;
    private final String nickname;
    private final String teamName;
    private final String profileImg;
    private final int age;
    private final String gender;
    private final String introduction;

    public static UserDto from(User user) {
        return UserDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .teamName(user.getTeam().getName())
                .profileImg(user.getProfileImg())
                .age(user.getAge())
                .gender(user.getGender().name())
                .introduction(user.getIntroduction())
                .build();
    }
}