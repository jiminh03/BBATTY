package com.ssafy.bbatty.domain.attendance.service;

import com.ssafy.bbatty.domain.attendance.dto.request.AttendanceVerifyRequest;
import com.ssafy.bbatty.domain.attendance.dto.response.AttendanceVerifyResponse;
import com.ssafy.bbatty.domain.attendance.entity.UserAttended;
import com.ssafy.bbatty.domain.attendance.repository.UserAttendedRepository;
import com.ssafy.bbatty.domain.attendance.repository.redis.ActiveGameRepository;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.constants.GameStatus;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.util.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAttendedRepository userAttendedRepository;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private ActiveGameRepository activeGameRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    @Test
    @DisplayName("직관 인증 성공")
    void verifyAttendance_Success() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        Long gameId = 1L;
        
        Team team = createTeam(teamId, "삼성 라이온즈");
        User user = createUser(userId, "testuser", team);
        
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();
        
        Game game = createGame(gameId, team, "대구삼성라이온즈파크",
                             LocalDateTime.now().plusHours(1), GameStatus.SCHEDULED);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeGameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(gameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED)))
                .thenReturn(List.of(game));
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        when(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).thenReturn(false);
        when(userAttendedRepository.save(any(UserAttended.class))).thenReturn(UserAttended.of(userId, gameId));
        doNothing().when(redisUtil).setValue(anyString(), anyString(), any(Duration.class));
        doNothing().when(redisUtil).addToSet(anyString(), anyString());
        doNothing().when(redisUtil).expire(anyString(), any(Duration.class));
        doNothing().when(activeGameRepository).cacheTeamGamesToday(eq(teamId), eq(LocalDate.now()), anyList());

        // When
        AttendanceVerifyResponse response = attendanceService.verifyAttendance(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.attendanceId()).isNotNull();
        assertThat(response.message()).isEqualTo("직관 인증이 완료되었습니다!");
        assertThat(response.gameInfo().gameId()).isEqualTo(gameId);
        assertThat(response.stadiumInfo().stadiumName()).isEqualTo("대구 삼성 라이온즈 파크");

        verify(userRepository).findById(userId);
        verify(userAttendedRepository).save(any(UserAttended.class));
        verify(redisUtil).setValue(anyString(), eq("ATTENDED"), any(Duration.class));
    }

    @Test
    @DisplayName("직관 인증 실패 - 사용자 없음")
    void verifyAttendance_UserNotFound() {
        // Given
        Long userId = 999L;
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
        verify(userAttendedRepository, never()).save(any(UserAttended.class));
    }

    @Test
    @DisplayName("직관 인증 실패 - 당일 경기 없음")
    void verifyAttendance_NoGameToday() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        
        Team team = createTeam(teamId, "삼성 라이온즈");
        User user = createUser(userId, "testuser", team);
        
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeGameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(gameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED)))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_GAME_TODAY);

        verify(userRepository).findById(userId);
        verify(gameRepository).findTeamGamesToday(eq(teamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED));
        verify(userAttendedRepository, never()).save(any(UserAttended.class));
    }

    @Test
    @DisplayName("직관 인증 실패 - 이미 인증함 (Redis)")
    void verifyAttendance_AlreadyAttendedRedis() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        Long gameId = 1L;
        
        Team team = createTeam(teamId, "삼성 라이온즈");
        User user = createUser(userId, "testuser", team);
        
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();
        
        Game game = createGame(gameId, team, "대구삼성라이온즈파크",
                             LocalDateTime.now().plusHours(1), GameStatus.SCHEDULED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeGameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(gameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED)))
                .thenReturn(List.of(game));
        when(redisUtil.hasKey(anyString())).thenReturn(true); // Redis에 이미 인증 정보 존재

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ATTENDED_GAME);

        verify(userRepository).findById(userId);
        verify(redisUtil).hasKey(anyString());
        verify(userAttendedRepository, never()).save(any(UserAttended.class));
    }

    @Test
    @DisplayName("직관 인증 실패 - 이미 인증함 (DB)")
    void verifyAttendance_AlreadyAttendedDB() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        Long gameId = 1L;
        
        Team team = createTeam(teamId, "삼성 라이온즈");
        User user = createUser(userId, "testuser", team);
        
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();
        
        Game game = createGame(gameId, team, "대구삼성라이온즈파크",
                             LocalDateTime.now().plusHours(1), GameStatus.SCHEDULED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeGameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(gameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED)))
                .thenReturn(List.of(game));
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        when(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).thenReturn(true); // DB에 이미 인증 정보 존재

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ATTENDED_GAME);

        verify(userRepository).findById(userId);
        verify(userAttendedRepository).existsByUserIdAndGameId(userId, gameId);
        verify(userAttendedRepository, never()).save(any(UserAttended.class));
    }

    @Test
    @DisplayName("직관 인증 실패 - 인증 시간 범위 밖 (너무 이른 시간)")
    void verifyAttendance_TooEarlyTime() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        Long gameId = 1L;
        
        Team team = createTeam(teamId, "삼성 라이온즈");
        User user = createUser(userId, "testuser", team);
        
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();
        
        // 경기 시작 3시간 후 경기 (인증 가능 시간은 2시간 전부터)
        Game game = createGame(gameId, team, "대구삼성라이온즈파크",
                             LocalDateTime.now().plusHours(3), GameStatus.SCHEDULED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeGameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(gameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED)))
                .thenReturn(List.of(game));

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ATTENDANCE_VALIDATION_FAILED);

        verify(userRepository).findById(userId);
        verify(userAttendedRepository, never()).save(any(UserAttended.class));
    }

    @Test
    @DisplayName("직관 인증 실패 - 위치 범위 밖")
    void verifyAttendance_OutOfRange() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        Long gameId = 1L;
        
        Team team = createTeam(teamId, "삼성 라이온즈");
        User user = createUser(userId, "testuser", team);
        
        // 대구에서 너무 먼 위치 (서울)
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("37.5665"))
                .longitude(new BigDecimal("126.9780"))
                .build();
        
        Game game = createGame(gameId, team, "대구삼성라이온즈파크",
                             LocalDateTime.now().plusHours(1), GameStatus.SCHEDULED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeGameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(gameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED)))
                .thenReturn(List.of(game));

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ATTENDANCE_VALIDATION_FAILED);

        verify(userRepository).findById(userId);
        verify(userAttendedRepository, never()).save(any(UserAttended.class));
    }

    @Test
    @DisplayName("직관 인증 실패 - 사용자 팀이 다른 경기장에서 경기")
    void verifyAttendance_DifferentStadium() {
        // Given
        Long userId = 1L;
        Long teamId = 1L;
        Long gameId = 1L;
        
        Team userTeam = createTeam(teamId, "삼성 라이온즈");
        User user = createUser(userId, "testuser", userTeam);
        
        // 삼성 라이온즈가 잠실에서 경기하는 상황 (홈구장이 아님)
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("37.5120"))  // 잠실 야구장 좌표
                .longitude(new BigDecimal("127.0721"))
                .build();
        
        Game game = createGameWithTeams(gameId, userTeam, createTeam(2L, "두산 베어스"), 
                                       "잠실야구장", LocalDateTime.now().plusHours(1), GameStatus.SCHEDULED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeGameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        when(gameRepository.findTeamGamesToday(eq(teamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED)))
                .thenReturn(List.of(game));
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        when(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).thenReturn(false);
        when(userAttendedRepository.save(any(UserAttended.class))).thenReturn(UserAttended.of(userId, gameId));
        doNothing().when(redisUtil).setValue(anyString(), anyString(), any(Duration.class));
        doNothing().when(redisUtil).addToSet(anyString(), anyString());
        doNothing().when(redisUtil).expire(anyString(), any(Duration.class));
        doNothing().when(activeGameRepository).cacheTeamGamesToday(eq(teamId), eq(LocalDate.now()), anyList());

        // When
        AttendanceVerifyResponse response = attendanceService.verifyAttendance(userId, request);

        // Then - 잠실에서 삼성 라이온즈 경기를 보러 간 경우 성공해야 함
        assertThat(response).isNotNull();
        assertThat(response.gameInfo().gameId()).isEqualTo(gameId);
        assertThat(response.stadiumInfo().stadiumName()).isEqualTo("잠실야구장");

        verify(userRepository).findById(userId);
        verify(userAttendedRepository).save(any(UserAttended.class));
    }

    @Test
    @DisplayName("직관 인증 실패 - 사용자가 다른 팀 팬인데 해당 팀 경기가 없음")
    void verifyAttendance_NoGameForUserTeam() {
        // Given
        Long userId = 1L;
        Long userTeamId = 1L;
        Long otherTeamId = 2L;
        
        Team userTeam = createTeam(userTeamId, "삼성 라이온즈");
        Team otherTeam = createTeam(otherTeamId, "두산 베어스");
        User user = createUser(userId, "testuser", userTeam);
        
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("37.5120"))
                .longitude(new BigDecimal("127.0721"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activeGameRepository.findTeamGamesToday(eq(userTeamId), eq(LocalDate.now())))
                .thenReturn(Optional.empty());
        // 사용자 팀(삼성)의 경기는 없고, 다른 팀들의 경기만 있는 상황
        when(gameRepository.findTeamGamesToday(eq(userTeamId), eq(LocalDate.now()), eq(GameStatus.SCHEDULED)))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_GAME_TODAY);

        verify(userRepository).findById(userId);
        verify(userAttendedRepository, never()).save(any(UserAttended.class));
    }

    private User createUser(Long id, String nickname, Team team) {
        return User.builder()
                .nickname(nickname)
                .gender(Gender.MALE)
                .birthYear(1998)
                .team(team)
                .introduction("테스트 소개")
                .build();
    }

    private Team createTeam(Long id, String name) {
        return Team.builder()
                .name(name)
                .build();
    }

    private Game createGame(Long id, Team homeTeam, String stadium, LocalDateTime dateTime, GameStatus status) {
        return Game.builder()
                .homeTeam(homeTeam)
                .awayTeam(homeTeam) // 테스트를 위해 같은 팀으로 설정
                .stadium(stadium)
                .dateTime(dateTime)
                .status(status)
                .doubleHeader(false)
                .build();
    }

    private Game createGameWithTeams(Long id, Team homeTeam, Team awayTeam, String stadium, LocalDateTime dateTime, GameStatus status) {
        return Game.builder()
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .stadium(stadium)
                .dateTime(dateTime)
                .status(status)
                .doubleHeader(false)
                .build();
    }
}