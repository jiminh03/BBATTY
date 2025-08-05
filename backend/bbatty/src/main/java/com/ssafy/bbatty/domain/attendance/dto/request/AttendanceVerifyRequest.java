package com.ssafy.bbatty.domain.attendance.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * 직관 인증 요청 DTO
 */
@Builder
public record AttendanceVerifyRequest(
        
        @NotNull(message = "위도는 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
        BigDecimal latitude,
        
        @NotNull(message = "경도는 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
        BigDecimal longitude
) {
    
    /**
     * 위도/경도 유효성 검증
     */
    public boolean isValidCoordinates() {
        return latitude != null && longitude != null &&
               latitude.compareTo(BigDecimal.valueOf(-90)) >= 0 &&
               latitude.compareTo(BigDecimal.valueOf(90)) <= 0 &&
               longitude.compareTo(BigDecimal.valueOf(-180)) >= 0 &&
               longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
    }
}
