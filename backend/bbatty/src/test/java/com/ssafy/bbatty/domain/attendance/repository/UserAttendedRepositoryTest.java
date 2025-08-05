package com.ssafy.bbatty.domain.attendance.repository;

import com.ssafy.bbatty.domain.attendance.entity.UserAttended;
import com.ssafy.bbatty.domain.attendance.entity.UserAttendedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAttendedRepository 테스트")
class UserAttendedRepositoryTest {

    @Mock
    private UserAttendedRepository userAttendedRepository;

    @Test
    @DisplayName("UserAttended 저장 테스트")
    void saveUserAttended() {
        // given
        Long userId = 1L;
        Long gameId = 100L;
        UserAttended userAttended = UserAttended.of(userId, gameId);
        
        given(userAttendedRepository.save(userAttended)).willReturn(userAttended);

        // when
        UserAttended savedUserAttended = userAttendedRepository.save(userAttended);

        // then
        assertThat(savedUserAttended.getUserId()).isEqualTo(userId);
        assertThat(savedUserAttended.getGameId()).isEqualTo(gameId);
        verify(userAttendedRepository).save(userAttended);
    }

    @Test
    @DisplayName("UserAttended 조회 테스트")
    void findUserAttended() {
        // given
        Long userId = 2L;
        Long gameId = 200L;
        UserAttended userAttended = UserAttended.of(userId, gameId);
        UserAttendedId id = new UserAttendedId(userId, gameId);
        
        given(userAttendedRepository.findById(id)).willReturn(Optional.of(userAttended));

        // when
        UserAttended foundUserAttended = userAttendedRepository.findById(id).orElse(null);

        // then
        assertThat(foundUserAttended).isNotNull();
        assertThat(foundUserAttended.getUserId()).isEqualTo(userId);
        assertThat(foundUserAttended.getGameId()).isEqualTo(gameId);
        verify(userAttendedRepository).findById(id);
    }

    @Test
    @DisplayName("존재하지 않는 UserAttended 조회 테스트")
    void findNonExistentUserAttended() {
        // given
        UserAttendedId id = new UserAttendedId(999L, 999L);
        
        given(userAttendedRepository.findById(id)).willReturn(Optional.empty());

        // when
        boolean exists = userAttendedRepository.findById(id).isPresent();

        // then
        assertThat(exists).isFalse();
        verify(userAttendedRepository).findById(id);
    }

    @Test
    @DisplayName("특정 사용자와 경기의 직관 인증 존재 여부 확인 - 존재하는 경우")
    void existsByUserIdAndGameId_exists() {
        // given
        Long userId = 3L;
        Long gameId = 300L;
        
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).willReturn(true);

        // when
        boolean exists = userAttendedRepository.existsByUserIdAndGameId(userId, gameId);

        // then
        assertThat(exists).isTrue();
        verify(userAttendedRepository).existsByUserIdAndGameId(userId, gameId);
    }

    @Test
    @DisplayName("특정 사용자와 경기의 직관 인증 존재 여부 확인 - 존재하지 않는 경우")
    void existsByUserIdAndGameId_notExists() {
        // given
        Long userId = 4L;
        Long gameId = 400L;
        
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).willReturn(false);

        // when
        boolean exists = userAttendedRepository.existsByUserIdAndGameId(userId, gameId);

        // then
        assertThat(exists).isFalse();
        verify(userAttendedRepository).existsByUserIdAndGameId(userId, gameId);
    }

    @Test
    @DisplayName("동일한 사용자가 다른 경기에 참석한 경우")
    void sameUserDifferentGame() {
        // given
        Long userId = 5L;
        Long gameId1 = 501L;
        Long gameId2 = 502L;
        
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId1)).willReturn(true);
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId2)).willReturn(true);
        given(userAttendedRepository.existsByUserIdAndGameId(userId, 999L)).willReturn(false);

        // when & then
        assertThat(userAttendedRepository.existsByUserIdAndGameId(userId, gameId1)).isTrue();
        assertThat(userAttendedRepository.existsByUserIdAndGameId(userId, gameId2)).isTrue();
        assertThat(userAttendedRepository.existsByUserIdAndGameId(userId, 999L)).isFalse();
        
        verify(userAttendedRepository).existsByUserIdAndGameId(userId, gameId1);
        verify(userAttendedRepository).existsByUserIdAndGameId(userId, gameId2);
        verify(userAttendedRepository).existsByUserIdAndGameId(userId, 999L);
    }

    @Test
    @DisplayName("다른 사용자가 동일한 경기에 참석한 경우")
    void differentUserSameGame() {
        // given
        Long userId1 = 6L;
        Long userId2 = 7L;
        Long gameId = 600L;
        
        given(userAttendedRepository.existsByUserIdAndGameId(userId1, gameId)).willReturn(true);
        given(userAttendedRepository.existsByUserIdAndGameId(userId2, gameId)).willReturn(true);
        given(userAttendedRepository.existsByUserIdAndGameId(999L, gameId)).willReturn(false);

        // when & then
        assertThat(userAttendedRepository.existsByUserIdAndGameId(userId1, gameId)).isTrue();
        assertThat(userAttendedRepository.existsByUserIdAndGameId(userId2, gameId)).isTrue();
        assertThat(userAttendedRepository.existsByUserIdAndGameId(999L, gameId)).isFalse();
        
        verify(userAttendedRepository).existsByUserIdAndGameId(userId1, gameId);
        verify(userAttendedRepository).existsByUserIdAndGameId(userId2, gameId);
        verify(userAttendedRepository).existsByUserIdAndGameId(999L, gameId);
    }

    @Test
    @DisplayName("UserAttended 삭제 테스트")
    void deleteUserAttended() {
        // given
        Long userId = 8L;
        Long gameId = 800L;
        UserAttendedId id = new UserAttendedId(userId, gameId);
        
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).willReturn(false);

        // when
        userAttendedRepository.deleteById(id);

        // then
        boolean exists = userAttendedRepository.existsByUserIdAndGameId(userId, gameId);
        assertThat(exists).isFalse();
        
        verify(userAttendedRepository).deleteById(id);
        verify(userAttendedRepository).existsByUserIdAndGameId(userId, gameId);
    }
}