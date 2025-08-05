package com.ssafy.bbatty.domain.attendance.util;

import com.ssafy.bbatty.global.constants.Attendance;
import com.ssafy.bbatty.global.constants.Stadium;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;

import java.math.BigDecimal;

/**
 * 위치 관련 유틸리티 클래스
 * - Haversine 공식을 이용한 거리 계산
 * - 경기장 범위 내 위치 검증
 */
public class LocationUtil {
    
    /**
     * 두 지점 간의 거리를 계산 (Haversine 공식)
     * 
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 거리 (킬로미터)
     */
    public static double calculateDistance(BigDecimal lat1, BigDecimal lon1, 
                                         BigDecimal lat2, BigDecimal lon2) {
        
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            throw new ApiException(ErrorCode.INVALID_COORDINATES);
        }
        
        try {
            // BigDecimal을 double로 변환
            double lat1Rad = Math.toRadians(lat1.doubleValue());
            double lon1Rad = Math.toRadians(lon1.doubleValue());
            double lat2Rad = Math.toRadians(lat2.doubleValue());
            double lon2Rad = Math.toRadians(lon2.doubleValue());
            
            // Haversine 공식 적용
            double deltaLat = lat2Rad - lat1Rad;
            double deltaLon = lon2Rad - lon1Rad;
            
            double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                      Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                      Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
            
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            
            return Attendance.EARTH_RADIUS_KM * c;
            
        } catch (Exception e) {
            throw new ApiException(ErrorCode.GPS_CALCULATION_ERROR);
        }
    }
    
    /**
     * 사용자가 특정 경기장 범위 내에 있는지 확인
     * 
     * @param userLat 사용자 위도
     * @param userLon 사용자 경도
     * @param stadium 경기장 정보
     * @return 범위 내 여부와 거리 정보
     */
    public static LocationValidationResult validateStadiumLocation(
            BigDecimal userLat, BigDecimal userLon, 
            Stadium stadium) {
        
        double distance = calculateDistance(
            userLat, userLon,
            stadium.getLatitude(), stadium.getLongitude()
        );
        
        boolean withinRange = distance <= Attendance.STADIUM_RADIUS_KM;
        
        return LocationValidationResult.of(withinRange, distance, stadium);
    }

    /**
     * 위치 검증 결과를 담는 레코드
     */
    public record LocationValidationResult(
            boolean withinRange,
            double distanceKm,
            String stadiumName,
            BigDecimal stadiumLatitude,
            BigDecimal stadiumLongitude
    ) {
        
        public static LocationValidationResult of(boolean withinRange, double distance, 
                                                Stadium stadium) {
            return new LocationValidationResult(
                withinRange,
                distance,
                stadium.getStadiumName(),
                stadium.getLatitude(),
                stadium.getLongitude()
            );
        }
        
        /**
         * 범위 밖인 경우 예외 발생
         */
        public void validateOrThrow() {
            if (!withinRange) {
                throw new ApiException(ErrorCode.NOT_IN_STADIUM);
            }
        }
        
        /**
         * 거리를 미터 단위로 반환
         */
        public double getDistanceMeters() {
            return distanceKm * 1000;
        }
    }
}
