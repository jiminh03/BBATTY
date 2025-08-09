package com.ssafy.bbatty.domain.attendance.service;

import com.ssafy.bbatty.domain.attendance.dto.redis.ActiveGame;
import com.ssafy.bbatty.domain.attendance.dto.request.AttendanceVerifyRequest;
import com.ssafy.bbatty.domain.attendance.dto.response.AttendanceVerifyResponse;
import com.ssafy.bbatty.domain.attendance.entity.UserAttended;
import com.ssafy.bbatty.domain.attendance.repository.UserAttendedRepository;
import com.ssafy.bbatty.domain.attendance.repository.redis.ActiveGameRepository;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.GameStatus;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.util.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AttendanceServiceImpl 테스트")
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
    @DisplayName("직관 인증 성공 테스트")
    void verifyAttendance_success() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        Long gameId = 100L;
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(BigDecimal.valueOf(37.5121), BigDecimal.valueOf(127.0718)); // 잠실야구장 근처

        User mockUser = createMockUser(userId, teamId);
        Team homeTeam = createMockTeam(teamId, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Game mockGame = createMockGame(gameId, homeTeam, awayTeam, LocalDateTime.now().plusHours(1));

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(gameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class), eq(GameStatus.SCHEDULED)))
                .willReturn(List.of(mockGame));
        given(activeGameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class)))
                .willReturn(Optional.empty());
        given(redisUtil.hasKey(anyString())).willReturn(false);
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).willReturn(false);
        given(userAttendedRepository.save(any(UserAttended.class))).willReturn(UserAttended.of(userId, gameId));

        // when
        AttendanceVerifyResponse response = attendanceService.verifyAttendance(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.attendanceId()).isNotNull();
        assertThat(response.gameInfo().gameId()).isEqualTo(gameId);
        
        verify(userAttendedRepository).save(any(UserAttended.class));
        verify(redisUtil).setValue(anyString(), eq("ATTENDED"), any(Duration.class));
        verify(redisUtil).addToSet(anyString(), anyString());
    }

    @Test
    @DisplayName("사용자를 찾을 수 없는 경우 예외 발생")
    void verifyAttendance_userNotFound() {
        // given
        Long userId = 999L;
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.9780));

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("당일 경기가 없는 경우 예외 발생")
    void verifyAttendance_noGameToday() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.9780));

        User mockUser = createMockUser(userId, teamId);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(activeGameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class)))
                .willReturn(Optional.empty());
        given(gameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class), eq(GameStatus.SCHEDULED)))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorCode.NO_GAME_TODAY.getMessage());
    }

    @Test
    @DisplayName("이미 인증한 경기인 경우 예외 발생 - DB 체크")
    void verifyAttendance_alreadyAttendedDB() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        Long gameId = 100L;
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(BigDecimal.valueOf(37.5121), BigDecimal.valueOf(127.0718)); // 잠실야구장 근처

        User mockUser = createMockUser(userId, teamId);
        Team homeTeam = createMockTeam(teamId, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Game mockGame = createMockGame(gameId, homeTeam, awayTeam, LocalDateTime.now().plusHours(1));

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(activeGameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class)))
                .willReturn(Optional.empty());
        given(gameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class), eq(GameStatus.SCHEDULED)))
                .willReturn(List.of(mockGame));
        given(redisUtil.hasKey(anyString())).willReturn(false);
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).willReturn(true); // DB에 이미 있음

        // when & then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorCode.ALREADY_ATTENDED_GAME.getMessage());
    }

    @Test
    @DisplayName("인증 조건을 만족하지 않는 경우 예외 발생")
    void verifyAttendance_validationFailed() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        Long gameId = 100L;
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(BigDecimal.valueOf(35.1796), BigDecimal.valueOf(129.0756)); // 부산 위치 (잠실과 거리 멀음)

        User mockUser = createMockUser(userId, teamId);
        Team homeTeam = createMockTeam(teamId, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Game mockGame = createMockGame(gameId, homeTeam, awayTeam, LocalDateTime.now().plusHours(1));

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(activeGameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class)))
                .willReturn(Optional.empty());
        given(gameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class), eq(GameStatus.SCHEDULED)))
                .willReturn(List.of(mockGame));
        given(redisUtil.hasKey(anyString())).willReturn(false);
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> attendanceService.verifyAttendance(userId, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorCode.ATTENDANCE_VALIDATION_FAILED.getMessage());
        
        // verify that game stadium was called for validation
        verify(mockGame, atLeastOnce()).getStadium();
    }

    @Test
    @DisplayName("Redis 캐시에서 경기 조회 성공")
    void getTeamGamesToday_cacheHit() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        Long gameId = 100L;
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(BigDecimal.valueOf(37.5121), BigDecimal.valueOf(127.0718));

        User mockUser = createMockUser(userId, teamId);
        Team homeTeam = createMockTeam(teamId, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Game mockGame = createMockGame(gameId, homeTeam, awayTeam, LocalDateTime.now().plusHours(1));

        ActiveGame activeGame = ActiveGame.builder()
                .gameId(gameId)
                .homeTeam("두산")
                .awayTeam("LG")
                .gameDateTime(LocalDateTime.now().plusHours(1))
                .stadium("잠실야구장")
                .status("SCHEDULED")
                .stadiumLatitude(BigDecimal.valueOf(37.5122))
                .stadiumLongitude(BigDecimal.valueOf(127.0721))
                .stadiumDisplayName("잠실야구장")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(activeGameRepository.findTeamGamesToday(eq(teamId), any(LocalDate.class)))
                .willReturn(Optional.of(List.of(activeGame)));
        given(gameRepository.findAllById(List.of(gameId))).willReturn(List.of(mockGame));
        given(redisUtil.hasKey(anyString())).willReturn(false);
        given(userAttendedRepository.existsByUserIdAndGameId(userId, gameId)).willReturn(false);
        given(userAttendedRepository.save(any(UserAttended.class))).willReturn(UserAttended.of(userId, gameId));

        // when
        AttendanceVerifyResponse response = attendanceService.verifyAttendance(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.attendanceId()).isNotNull();
        verify(gameRepository, never()).findTeamGamesToday(any(), any(), any()); // DB 조회 없음
        verify(gameRepository).findAllById(List.of(gameId)); // ID로 조회만 함
    }

    private User createMockUser(Long userId, Long teamId) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        given(user.getTeamId()).willReturn(teamId);
        return user;
    }

    private Team createMockTeam(Long teamId, String teamName) {
        Team team = mock(Team.class);
        given(team.getId()).willReturn(teamId);
        given(team.getName()).willReturn(teamName);
        return team;
    }

    private Game createMockGame(Long gameId, Team homeTeam, Team awayTeam, LocalDateTime dateTime) {
        Game game = mock(Game.class);
        given(game.getId()).willReturn(gameId);
        given(game.getHomeTeam()).willReturn(homeTeam);
        given(game.getAwayTeam()).willReturn(awayTeam);
        given(game.getDateTime()).willReturn(dateTime);
        given(game.getStatus()).willReturn(GameStatus.SCHEDULED);
        given(game.getStadium()).willReturn("잠실야구장");
        given(game.getLatitude()).willReturn(BigDecimal.valueOf(37.5121528));
        given(game.getLongitude()).willReturn(BigDecimal.valueOf(127.0717917));
        return game;
    }
}