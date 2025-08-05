package com.ssafy.bbatty.domain.attendance.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserAttendedId 복합키 테스트")
class UserAttendedIdTest {

    @Test
    @DisplayName("UserAttendedId 기본 생성자 테스트")
    void createDefaultUserAttendedId() {
        // when
        UserAttendedId id = new UserAttendedId();

        // then
        assertThat(id).isNotNull();
    }

    @Test
    @DisplayName("UserAttendedId 전체 생성자 테스트")
    void createUserAttendedIdWithAllArgs() {
        // given
        Long userId = 1L;
        Long gameId = 100L;

        // when
        UserAttendedId id = new UserAttendedId(userId, gameId);

        // then
        assertThat(id).isNotNull();
    }

    @Test
    @DisplayName("UserAttendedId equals 메서드 테스트")
    void equalsTest() {
        // given
        Long userId = 1L;
        Long gameId = 100L;
        UserAttendedId id1 = new UserAttendedId(userId, gameId);
        UserAttendedId id2 = new UserAttendedId(userId, gameId);
        UserAttendedId id3 = new UserAttendedId(2L, 200L);

        // then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(null);
        assertThat(id1).isEqualTo(id1);
    }

    @Test
    @DisplayName("UserAttendedId hashCode 메서드 테스트")
    void hashCodeTest() {
        // given
        Long userId = 1L;
        Long gameId = 100L;
        UserAttendedId id1 = new UserAttendedId(userId, gameId);
        UserAttendedId id2 = new UserAttendedId(userId, gameId);

        // then
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("서로 다른 UserAttendedId의 hashCode는 다를 수 있다")
    void differentHashCodeTest() {
        // given
        UserAttendedId id1 = new UserAttendedId(1L, 100L);
        UserAttendedId id2 = new UserAttendedId(2L, 200L);

        // then
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode());
    }
}