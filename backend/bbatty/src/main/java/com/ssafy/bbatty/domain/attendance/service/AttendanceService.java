package com.ssafy.bbatty.domain.attendance.service;

import com.ssafy.bbatty.domain.attendance.dto.request.AttendanceVerifyRequest;
import com.ssafy.bbatty.domain.attendance.dto.response.AttendanceVerifyResponse;

/**
 * 직관 인증 서비스 인터페이스
 */
public interface AttendanceService {
    
    /**
     * 직관 인증 처리
     * 
     * @param userId 사용자 ID (JWT에서 추출)
     * @param request 위치 정보 요청
     * @return 인증 결과
     */
    AttendanceVerifyResponse verifyAttendance(Long userId, AttendanceVerifyRequest request);
}
