package com.ssafy.bbatty.domain.attendance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.attendance.dto.request.AttendanceVerifyRequest;
import com.ssafy.bbatty.domain.attendance.dto.response.AttendanceVerifyResponse;
import com.ssafy.bbatty.domain.attendance.service.AttendanceService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private AttendanceService attendanceService;

    @Test
    @DisplayName("직관 인증 API 성공")
    @WithMockUser
    void verifyAttendance_Success() throws Exception {
        // Given
        Long userId = 1L;
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        AttendanceVerifyResponse.GameInfo gameInfo = AttendanceVerifyResponse.GameInfo.builder()
                .gameId(1L)
                .homeTeam("삼성 라이온즈")
                .awayTeam("두산 베어스")
                .gameDateTime(LocalDateTime.now().plusHours(2))
                .status("SCHEDULED")
                .build();

        AttendanceVerifyResponse.StadiumInfo stadiumInfo = AttendanceVerifyResponse.StadiumInfo.builder()
                .stadiumName("대구 삼성 라이온즈 파크")
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .distanceFromUser(50.0)
                .build();

        AttendanceVerifyResponse expectedResponse = AttendanceVerifyResponse.builder()
                .attendanceId("20241201_USER1_GAME1")
                .message("직관 인증이 완료되었습니다!")
                .attendanceTime(LocalDateTime.now())
                .validUntil(LocalDateTime.now().withHour(23).withMinute(59).withSecond(59))
                .gameInfo(gameInfo)
                .stadiumInfo(stadiumInfo)
                .build();

        when(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .thenReturn(expectedResponse);

        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "MALE", 1L);

        // When & Then
        mockMvc.perform(post("/api/attendance/verify")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attendanceId").value("20241201_USER1_GAME1"))
                .andExpect(jsonPath("$.data.message").value("직관 인증이 완료되었습니다!"))
                .andExpect(jsonPath("$.data.gameInfo.gameId").value(1))
                .andExpect(jsonPath("$.data.gameInfo.homeTeam").value("삼성 라이온즈"))
                .andExpect(jsonPath("$.data.stadiumInfo.stadiumName").value("대구 삼성 라이온즈 파크"))
                .andExpect(jsonPath("$.data.stadiumInfo.distanceFromUser").value(50.0));
    }

    @Test
    @DisplayName("직관 인증 API 실패 - 잘못된 위도")
    @WithMockUser
    void verifyAttendance_InvalidLatitude() throws Exception {
        // Given
        Long userId = 1L;
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("999.0")) // 잘못된 위도
                .longitude(new BigDecimal("128.6819"))
                .build();

        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "MALE", 1L);

        // When & Then
        mockMvc.perform(post("/api/attendance/verify")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("직관 인증 API 실패 - 잘못된 경도")
    @WithMockUser
    void verifyAttendance_InvalidLongitude() throws Exception {
        // Given
        Long userId = 1L;
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("999.0")) // 잘못된 경도
                .build();

        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "MALE", 1L);

        // When & Then
        mockMvc.perform(post("/api/attendance/verify")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("직관 인증 API 실패 - 필수 값 누락")
    @WithMockUser
    void verifyAttendance_MissingRequired() throws Exception {
        // Given
        Long userId = 1L;
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(null) // 필수 값 누락
                .longitude(new BigDecimal("128.6819"))
                .build();

        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "MALE", 1L);

        // When & Then
        mockMvc.perform(post("/api/attendance/verify")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("직관 인증 API 실패 - 사용자 없음")
    @WithMockUser
    void verifyAttendance_UserNotFound() throws Exception {
        // Given
        Long userId = 999L;
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        when(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .thenThrow(new ApiException(ErrorCode.USER_NOT_FOUND));

        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "MALE", 1L);

        // When & Then
        mockMvc.perform(post("/api/attendance/verify")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("직관 인증 API 실패 - 당일 경기 없음")
    @WithMockUser
    void verifyAttendance_NoGameToday() throws Exception {
        // Given
        Long userId = 1L;
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        when(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .thenThrow(new ApiException(ErrorCode.NO_GAME_TODAY));

        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "MALE", 1L);

        // When & Then
        mockMvc.perform(post("/api/attendance/verify")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("직관 인증 API 실패 - 이미 인증함")
    @WithMockUser
    void verifyAttendance_AlreadyAttended() throws Exception {
        // Given
        Long userId = 1L;
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        when(attendanceService.verifyAttendance(eq(userId), any(AttendanceVerifyRequest.class)))
                .thenThrow(new ApiException(ErrorCode.ALREADY_ATTENDED_GAME));

        UserPrincipal userPrincipal = new UserPrincipal(userId, 25, "MALE", 1L);

        // When & Then
        mockMvc.perform(post("/api/attendance/verify")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}