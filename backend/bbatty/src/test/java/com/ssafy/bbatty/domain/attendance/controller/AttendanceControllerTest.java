package com.ssafy.bbatty.domain.attendance.controller;

import com.ssafy.bbatty.domain.attendance.dto.request.AttendanceVerifyRequest;
import com.ssafy.bbatty.domain.attendance.dto.response.AttendanceVerifyResponse;
import com.ssafy.bbatty.domain.attendance.service.AttendanceService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Status;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceController 테스트")
class AttendanceControllerTest {

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private AttendanceController attendanceController;

    @Test
    @DisplayName("직관 인증 API 성공 테스트")
    void verifyAttendance_success() {
        // given
        Long userId = 1L;
        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "M", 1L);
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(
                BigDecimal.valueOf(37.5665), 
                BigDecimal.valueOf(126.9780)
        );
        
        AttendanceVerifyResponse.GameInfo gameInfo = AttendanceVerifyResponse.GameInfo.builder()
                .gameId(100L)
                .homeTeam("두산")
                .awayTeam("LG")
                .gameDateTime(LocalDateTime.now().plusHours(1))
                .status("SCHEDULED")
                .build();
        
        AttendanceVerifyResponse.StadiumInfo stadiumInfo = AttendanceVerifyResponse.StadiumInfo.builder()
                .stadiumName("잠실야구장")
                .latitude(BigDecimal.valueOf(37.5122))
                .longitude(BigDecimal.valueOf(127.0721))
                .distanceFromUser(50.0)
                .build();
        
        AttendanceVerifyResponse mockResponse = AttendanceVerifyResponse.success(
                "20250805_USER1_GAME100",
                gameInfo,
                stadiumInfo
        );

        given(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .willReturn(mockResponse);

        // when
        ApiResponse<AttendanceVerifyResponse> response = attendanceController.verifyAttendance(userPrincipal, request).getBody();

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(Status.SUCCESS);
        assertThat(response.data()).isEqualTo(mockResponse);
        assertThat(response.data().attendanceId()).isEqualTo("20250805_USER1_GAME100");
        assertThat(response.data().gameInfo().gameId()).isEqualTo(100L);
        
        verify(attendanceService).verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class));
    }

    @Test
    @DisplayName("사용자 없음 예외 처리 테스트")
    void verifyAttendance_userNotFound() {
        // given
        Long userId = 999L;
        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "M", 1L);
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(
                BigDecimal.valueOf(37.5665), 
                BigDecimal.valueOf(126.9780)
        );

        given(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .willThrow(new ApiException(ErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> attendanceController.verifyAttendance(userPrincipal, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

        verify(attendanceService).verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class));
    }

    @Test
    @DisplayName("당일 경기 없음 예외 처리 테스트")
    void verifyAttendance_noGameToday() {
        // given
        Long userId = 1L;
        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "M", 1L);
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(
                BigDecimal.valueOf(37.5665), 
                BigDecimal.valueOf(126.9780)
        );

        given(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .willThrow(new ApiException(ErrorCode.NO_GAME_TODAY));

        // when & then
        assertThatThrownBy(() -> attendanceController.verifyAttendance(userPrincipal, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorCode.NO_GAME_TODAY.getMessage());

        verify(attendanceService).verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class));
    }

    @Test
    @DisplayName("이미 인증한 경기 예외 처리 테스트")
    void verifyAttendance_alreadyAttended() {
        // given
        Long userId = 1L;
        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "M", 1L);
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(
                BigDecimal.valueOf(37.5665), 
                BigDecimal.valueOf(126.9780)
        );

        given(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .willThrow(new ApiException(ErrorCode.ALREADY_ATTENDED_GAME));

        // when & then
        assertThatThrownBy(() -> attendanceController.verifyAttendance(userPrincipal, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorCode.ALREADY_ATTENDED_GAME.getMessage());

        verify(attendanceService).verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class));
    }

    @Test  
    @DisplayName("직관 인증 검증 실패 예외 처리 테스트")
    void verifyAttendance_validationFailed() {
        // given
        Long userId = 1L;
        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "M", 1L);
        AttendanceVerifyRequest request = new AttendanceVerifyRequest(
                BigDecimal.valueOf(35.1796), 
                BigDecimal.valueOf(129.0756)
        ); // 부산 위치

        given(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .willThrow(new ApiException(ErrorCode.ATTENDANCE_VALIDATION_FAILED));

        // when & then
        assertThatThrownBy(() -> attendanceController.verifyAttendance(userPrincipal, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorCode.ATTENDANCE_VALIDATION_FAILED.getMessage());

        verify(attendanceService).verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class));
    }
}