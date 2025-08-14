package com.ssafy.bbatty.domain.user.dto.response;

import com.ssafy.bbatty.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long id;
    private String nickname;
    private String profileImg;
    private String introduction;
    
    // 프라이버시 설정 (본인만 볼 수 있음)
    private Boolean postsPublic;
    private Boolean statsPublic;
    private Boolean attendanceRecordsPublic;
    
    // 알림 설정 (본인만 볼 수 있음)
    private Boolean trafficSpikeAlertEnabled;

    public static UserResponseDto from(User user, boolean isOwnProfile) {
        UserResponseDtoBuilder builder = UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .introduction(user.getIntroduction());

        // 본인 프로필인 경우에만 프라이버시 설정 및 알림 설정 포함
        if (isOwnProfile) {
            builder.postsPublic(user.getPostsPublic())
                   .statsPublic(user.getStatsPublic())
                   .attendanceRecordsPublic(user.getAttendanceRecordsPublic())
                   .trafficSpikeAlertEnabled(user.getTrafficSpikeAlertEnabled());
        }

        return builder.build();
    }
}
