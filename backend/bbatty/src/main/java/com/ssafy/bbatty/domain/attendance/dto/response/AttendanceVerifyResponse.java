package com.ssafy.bbatty.domain.attendance.dto.response;

import com.ssafy.bbatty.global.util.DateUtil;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 직관 인증 성공 응답 DTO
 */
@Builder
public record AttendanceVerifyResponse(
        
        // 인증 정보
        String attendanceId,
        String message,
        LocalDateTime attendanceTime,
        LocalDateTime validUntil,
        
        // 경기 정보
        GameInfo gameInfo,
        
        // 경기장 정보
        StadiumInfo stadiumInfo
) {
    
    /**
     * 경기 정보 중첩 DTO
     */
    @Builder
    public record GameInfo(
            Long gameId,
            String homeTeam,
            String awayTeam,
            LocalDateTime gameDateTime,
            String status
    ) {}
    
    /**
     * 경기장 정보 중첩 DTO
     */
    @Builder
    public record StadiumInfo(
            String stadiumName,
            BigDecimal latitude,
            BigDecimal longitude,
            Double distanceFromUser
    ) {}
    
    /**
     * 성공 응답 생성 팩토리 메서드
     */
    public static AttendanceVerifyResponse success(String attendanceId, GameInfo gameInfo, StadiumInfo stadiumInfo) {
        return AttendanceVerifyResponse.builder()
                .attendanceId(attendanceId)
                .message("직관 인증이 완료되었습니다!")
                .attendanceTime(LocalDateTime.now())
                .validUntil(DateUtil.endOfToday())
                .gameInfo(gameInfo)
                .stadiumInfo(stadiumInfo)
                .build();
    }
}
