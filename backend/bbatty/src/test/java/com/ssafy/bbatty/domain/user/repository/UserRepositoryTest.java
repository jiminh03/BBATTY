package com.ssafy.bbatty.domain.user.repository;

import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.constants.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private User createTestUser(String nickname, Long id) {
        Team team = Team.builder()
                .id(1L)
                .name("LG 트윈스")
                .build();

        return User.builder()
                .id(id)
                .nickname(nickname)
                .team(team)
                .gender(Gender.MALE)
                .birthYear(1990)
                .profileImg("profile.jpg")
                .introduction("테스트 소개")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("닉네임으로 사용자 존재 여부 확인 - 존재하는 경우")
    void existsByNickname_UserExists_ReturnTrue() {
        // given
        String nickname = "테스트유저";
        given(userRepository.existsByNickname(nickname)).willReturn(true);

        // when
        boolean exists = userRepository.existsByNickname(nickname);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("닉네임으로 사용자 존재 여부 확인 - 존재하지 않는 경우")
    void existsByNickname_UserNotExists_ReturnFalse() {
        // given
        String nickname = "존재하지않는닉네임";
        given(userRepository.existsByNickname(nickname)).willReturn(false);

        // when
        boolean exists = userRepository.existsByNickname(nickname);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("닉네임으로 사용자 조회 - 존재하는 경우")
    void findByNickname_UserExists_ReturnUser() {
        // given
        String nickname = "테스트유저";
        User expectedUser = createTestUser(nickname, 1L);
        given(userRepository.findByNickname(nickname)).willReturn(Optional.of(expectedUser));

        // when
        Optional<User> foundUser = userRepository.findByNickname(nickname);

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(expectedUser.getId());
        assertThat(foundUser.get().getNickname()).isEqualTo(nickname);
        assertThat(foundUser.get().getGender()).isEqualTo(Gender.MALE);
        assertThat(foundUser.get().getBirthYear()).isEqualTo(1990);
    }

    @Test
    @DisplayName("닉네임으로 사용자 조회 - 존재하지 않는 경우")
    void findByNickname_UserNotExists_ReturnEmpty() {
        // given
        String nickname = "존재하지않는닉네임";
        given(userRepository.findByNickname(nickname)).willReturn(Optional.empty());

        // when
        Optional<User> foundUser = userRepository.findByNickname(nickname);

        // then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("닉네임 대소문자 구분 테스트")
    void findByNickname_CaseSensitive() {
        // given
        User expectedUser = createTestUser("TestUser", 1L);
        given(userRepository.findByNickname("TestUser")).willReturn(Optional.of(expectedUser));
        given(userRepository.findByNickname("testuser")).willReturn(Optional.empty());
        given(userRepository.findByNickname("TESTUSER")).willReturn(Optional.empty());

        // when
        Optional<User> foundUser1 = userRepository.findByNickname("TestUser");
        Optional<User> foundUser2 = userRepository.findByNickname("testuser");
        Optional<User> foundUser3 = userRepository.findByNickname("TESTUSER");

        // then
        assertThat(foundUser1).isPresent();
        assertThat(foundUser2).isEmpty();
        assertThat(foundUser3).isEmpty();
    }

    @Test
    @DisplayName("여러 사용자가 있을 때 특정 닉네임 조회")
    void findByNickname_MultipleUsers_ReturnCorrectUser() {
        // given
        User targetUser = createTestUser("사용자2", 2L);
        given(userRepository.findByNickname("사용자2")).willReturn(Optional.of(targetUser));

        // when
        Optional<User> foundUser = userRepository.findByNickname("사용자2");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getNickname()).isEqualTo("사용자2");
        assertThat(foundUser.get().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 다른 사용자들이 있을 때")
    void existsByNickname_WithMultipleUsers() {
        // given
        given(userRepository.existsByNickname("존재하는닉네임")).willReturn(true);
        given(userRepository.existsByNickname("다른닉네임")).willReturn(true);
        given(userRepository.existsByNickname("존재하지않는닉네임")).willReturn(false);

        // when
        boolean exists1 = userRepository.existsByNickname("존재하는닉네임");
        boolean exists2 = userRepository.existsByNickname("다른닉네임");
        boolean exists3 = userRepository.existsByNickname("존재하지않는닉네임");

        // then
        assertThat(exists1).isTrue();
        assertThat(exists2).isTrue();
        assertThat(exists3).isFalse();
    }

    @Test
    @DisplayName("사용자 저장 및 조회 동작 확인")
    void saveAndFind_MethodCallVerification() {
        // given
        User user = createTestUser("통합테스트유저", null);
        User savedUser = createTestUser("통합테스트유저", 1L);
        
        given(userRepository.save(user)).willReturn(savedUser);
        given(userRepository.findByNickname("통합테스트유저")).willReturn(Optional.of(savedUser));
        given(userRepository.existsByNickname("통합테스트유저")).willReturn(true);

        // when
        User result = userRepository.save(user);
        Optional<User> foundUser = userRepository.findByNickname("통합테스트유저");
        boolean exists = userRepository.existsByNickname("통합테스트유저");

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNickname()).isEqualTo("통합테스트유저");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getNickname()).isEqualTo("통합테스트유저");
        assertThat(exists).isTrue();
    }
}