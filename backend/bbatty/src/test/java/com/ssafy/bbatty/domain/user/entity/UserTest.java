package com.ssafy.bbatty.domain.user.entity;

import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.constants.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 엔티티 테스트")
class UserTest {

    private Team createTestTeam() {
        return Team.builder()
                .id(1L)
                .name("LG 트윈스")
                .build();
    }

    @Test
    @DisplayName("User 생성 - 팩토리 메서드 사용")
    void createUser_Success() {
        // given
        String nickname = "테스트유저";
        Team team = createTestTeam();
        Gender gender = Gender.MALE;
        Integer birthYear = 1990;
        String profileImg = "profile.jpg";
        String introduction = "안녕하세요";
        Role role = Role.USER;

        // when
        User user = User.createUser(nickname, team, gender, birthYear, profileImg, introduction, role);

        // then
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getTeam()).isEqualTo(team);
        assertThat(user.getGender()).isEqualTo(gender);
        assertThat(user.getBirthYear()).isEqualTo(birthYear);
        assertThat(user.getProfileImg()).isEqualTo(profileImg);
        assertThat(user.getIntroduction()).isEqualTo(introduction);
        assertThat(user.getRole()).isEqualTo(role);
        assertThat(user.getPostsPublic()).isTrue(); // 기본값
        assertThat(user.getStatsPublic()).isTrue(); // 기본값
        assertThat(user.getAttendanceRecordsPublic()).isTrue(); // 기본값
    }

    @Test
    @DisplayName("User 빌더로 생성 - 기본값 확인")
    void createUser_BuilderWithDefaults_Success() {
        // given
        Team team = createTestTeam();

        // when
        User user = User.builder()
                .nickname("테스트유저")
                .team(team)
                .gender(Gender.FEMALE)
                .birthYear(1995)
                .build();

        // then
        assertThat(user.getRole()).isEqualTo(Role.USER); // 기본값
        assertThat(user.getPostsPublic()).isTrue(); // 기본값
        assertThat(user.getStatsPublic()).isTrue(); // 기본값
        assertThat(user.getAttendanceRecordsPublic()).isTrue(); // 기본값
    }

    @Test
    @DisplayName("프로필 업데이트 - 성공")
    void updateProfile_Success() {
        // given
        Team team = createTestTeam();
        User user = User.builder()
                .nickname("기존닉네임")
                .team(team)
                .gender(Gender.MALE)
                .birthYear(1990)
                .profileImg("old_profile.jpg")
                .introduction("기존 소개")
                .build();

        String newNickname = "새닉네임";
        String newIntroduction = "새로운 소개";
        String newProfileImg = "new_profile.jpg";

        // when
        user.updateProfile(newNickname, newIntroduction, newProfileImg);

        // then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getIntroduction()).isEqualTo(newIntroduction);
        assertThat(user.getProfileImg()).isEqualTo(newProfileImg);
    }

    @Test
    @DisplayName("프라이버시 설정 업데이트 - 성공")
    void updatePrivacySettings_Success() {
        // given
        Team team = createTestTeam();
        User user = User.builder()
                .nickname("테스트유저")
                .team(team)
                .gender(Gender.MALE)
                .birthYear(1990)
                .build();

        // when
        user.updatePrivacySettings(false, false, false);

        // then
        assertThat(user.getPostsPublic()).isFalse();
        assertThat(user.getStatsPublic()).isFalse();
        assertThat(user.getAttendanceRecordsPublic()).isFalse();
    }

    @Test
    @DisplayName("나이 계산 - 정확한 나이 반환")
    void getAge_CorrectAge() {
        // given
        int currentYear = LocalDate.now().getYear();
        int birthYear = 1990;
        int expectedAge = currentYear - birthYear + 1; // 한국식 나이

        Team team = createTestTeam();
        User user = User.builder()
                .nickname("테스트유저")
                .team(team)
                .gender(Gender.MALE)
                .birthYear(birthYear)
                .build();

        // when
        int actualAge = user.getAge();

        // then
        assertThat(actualAge).isEqualTo(expectedAge);
    }

    @Test
    @DisplayName("팀 ID 반환 - 팀이 있는 경우")
    void getTeamId_WithTeam_ReturnTeamId() {
        // given
        Team team = createTestTeam();
        User user = User.builder()
                .nickname("테스트유저")
                .team(team)
                .gender(Gender.MALE)
                .birthYear(1990)
                .build();

        // when
        Long teamId = user.getTeamId();

        // then
        assertThat(teamId).isEqualTo(team.getId());
    }

    @Test
    @DisplayName("팀 ID 반환 - 팀이 없는 경우")
    void getTeamId_WithoutTeam_ReturnNull() {
        // given
        User user = User.builder()
                .nickname("테스트유저")
                .team(null)
                .gender(Gender.MALE)
                .birthYear(1990)
                .build();

        // when
        Long teamId = user.getTeamId();

        // then
        assertThat(teamId).isNull();
    }

    @Test
    @DisplayName("프로필 업데이트 - null 값 처리")
    void updateProfile_WithNullValues_Success() {
        // given
        Team team = createTestTeam();
        User user = User.builder()
                .nickname("기존닉네임")
                .team(team)
                .gender(Gender.MALE)
                .birthYear(1990)
                .profileImg("old_profile.jpg")
                .introduction("기존 소개")
                .build();

        // when
        user.updateProfile("새닉네임", null, null);

        // then
        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getIntroduction()).isNull();
        assertThat(user.getProfileImg()).isNull();
    }

    @Test
    @DisplayName("프라이버시 설정 업데이트 - null 값 처리")
    void updatePrivacySettings_WithNullValues_Success() {
        // given
        Team team = createTestTeam();
        User user = User.builder()
                .nickname("테스트유저")
                .team(team)
                .gender(Gender.MALE)
                .birthYear(1990)
                .postsPublic(true)
                .statsPublic(true)
                .attendanceRecordsPublic(true)
                .build();

        // when
        user.updatePrivacySettings(null, false, null);

        // then
        assertThat(user.getPostsPublic()).isNull();
        assertThat(user.getStatsPublic()).isFalse();
        assertThat(user.getAttendanceRecordsPublic()).isNull();
    }
}