package com.ssafy.bbatty.domain.attendance.controller;

import com.ssafy.bbatty.domain.attendance.dto.request.AttendanceVerifyRequest;
import com.ssafy.bbatty.domain.attendance.dto.response.AttendanceVerifyResponse;
import com.ssafy.bbatty.domain.attendance.service.AttendanceService;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 직관 인증 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    
    private final AttendanceService attendanceService;
    
    /**
     * 직관 인증 API
     * 
     * @param userPrincipal JWT에서 추출한 사용자 정보
     * @param request 위치 정보 요청
     * @return 인증 결과
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AttendanceVerifyResponse>> verifyAttendance(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AttendanceVerifyRequest request) {
        
        Long userId = userPrincipal.getUserId();
        log.info("직관 인증 요청 - userId: {}", userId);
        
        AttendanceVerifyResponse response = attendanceService.verifyAttendance(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
