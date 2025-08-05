package com.ssafy.bbatty.domain.attendance.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserAttended 엔티티 테스트")
class UserAttendedTest {

    @Test
    @DisplayName("UserAttended 생성자 테스트")
    void createUserAttended() {
        // given
        Long userId = 1L;
        Long gameId = 100L;

        // when
        UserAttended userAttended = UserAttended.builder()
                .userId(userId)
                .gameId(gameId)
                .build();

        // then
        assertThat(userAttended.getUserId()).isEqualTo(userId);
        assertThat(userAttended.getGameId()).isEqualTo(gameId);
    }

    @Test
    @DisplayName("UserAttended 팩토리 메서드 테스트")
    void createUserAttendedByFactory() {
        // given
        Long userId = 2L;
        Long gameId = 200L;

        // when
        UserAttended userAttended = UserAttended.of(userId, gameId);

        // then
        assertThat(userAttended.getUserId()).isEqualTo(userId);
        assertThat(userAttended.getGameId()).isEqualTo(gameId);
    }

    @Test
    @DisplayName("UserAttended 불변성 테스트 - setter 메서드 없음")
    void immutableTest() {
        // given
        Long userId = 3L;
        Long gameId = 300L;
        UserAttended userAttended = UserAttended.of(userId, gameId);

        // then
        assertThat(userAttended.getUserId()).isEqualTo(userId);
        assertThat(userAttended.getGameId()).isEqualTo(gameId);
    }
}