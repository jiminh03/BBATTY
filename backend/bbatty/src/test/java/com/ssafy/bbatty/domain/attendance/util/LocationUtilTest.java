package com.ssafy.bbatty.domain.attendance.util;

import com.ssafy.bbatty.global.constants.Stadium;
import com.ssafy.bbatty.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationUtilTest {

    @Test
    @DisplayName("거리 계산 - 같은 위치")
    void calculateDistance_SameLocation() {
        // Given
        BigDecimal lat = new BigDecimal("35.8411");
        BigDecimal lon = new BigDecimal("128.6819");

        // When
        double distance = LocationUtil.calculateDistance(lat, lon, lat, lon);

        // Then
        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    @DisplayName("거리 계산 - 서로 다른 위치")
    void calculateDistance_DifferentLocation() {
        // Given - 대구와 서울의 좌표
        BigDecimal daeguLat = new BigDecimal("35.8411");
        BigDecimal daeguLon = new BigDecimal("128.6819");
        BigDecimal seoulLat = new BigDecimal("37.5665");
        BigDecimal seoulLon = new BigDecimal("126.9780");

        // When
        double distance = LocationUtil.calculateDistance(daeguLat, daeguLon, seoulLat, seoulLon);

        // Then
        assertThat(distance).isGreaterThan(200); // 대구-서울 간 거리는 약 300km
        assertThat(distance).isLessThan(400);
    }

    @Test
    @DisplayName("거리 계산 - null 위도")
    void calculateDistance_NullLatitude() {
        // Given
        BigDecimal lat = null;
        BigDecimal lon = new BigDecimal("128.6819");
        BigDecimal targetLat = new BigDecimal("35.8411");
        BigDecimal targetLon = new BigDecimal("128.6819");

        // When & Then
        assertThatThrownBy(() -> LocationUtil.calculateDistance(lat, lon, targetLat, targetLon))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("거리 계산 - null 경도")
    void calculateDistance_NullLongitude() {
        // Given
        BigDecimal lat = new BigDecimal("35.8411");
        BigDecimal lon = null;
        BigDecimal targetLat = new BigDecimal("35.8411");
        BigDecimal targetLon = new BigDecimal("128.6819");

        // When & Then
        assertThatThrownBy(() -> LocationUtil.calculateDistance(lat, lon, targetLat, targetLon))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("경기장 위치 검증 - 범위 내")
    void validateStadiumLocation_WithinRange() {
        // Given - 대구 삼성 라이온즈 파크 근처 좌표
        BigDecimal userLat = new BigDecimal("35.8411");
        BigDecimal userLon = new BigDecimal("128.6819");
        Stadium stadium = Stadium.DAEGU;

        // When
        LocationUtil.LocationValidationResult result = 
                LocationUtil.validateStadiumLocation(userLat, userLon, stadium);

        // Then
        assertThat(result.withinRange()).isTrue();
        assertThat(result.distanceKm()).isLessThan(0.15); // 150m 이내
        assertThat(result.stadiumName()).isEqualTo("대구삼성라이온즈파크");
        assertThat(result.getDistanceMeters()).isLessThan(150);
    }

    @Test
    @DisplayName("경기장 위치 검증 - 범위 밖")
    void validateStadiumLocation_OutOfRange() {
        // Given - 서울 좌표 (대구 삼성 라이온즈 파크에서 멀리 떨어진 곳)
        BigDecimal userLat = new BigDecimal("37.5665");
        BigDecimal userLon = new BigDecimal("126.9780");
        Stadium stadium = Stadium.DAEGU;

        // When
        LocationUtil.LocationValidationResult result = 
                LocationUtil.validateStadiumLocation(userLat, userLon, stadium);

        // Then
        assertThat(result.withinRange()).isFalse();
        assertThat(result.distanceKm()).isGreaterThan(0.15); // 150m 초과
        assertThat(result.stadiumName()).isEqualTo("대구삼성라이온즈파크");
        assertThat(result.getDistanceMeters()).isGreaterThan(150);
    }

    @Test
    @DisplayName("LocationValidationResult - validateOrThrow 성공")
    void locationValidationResult_ValidateOrThrow_Success() {
        // Given
        LocationUtil.LocationValidationResult result = 
                new LocationUtil.LocationValidationResult(
                    true, 
                    0.1, 
                    "테스트 경기장",
                    new BigDecimal("35.8411"),
                    new BigDecimal("128.6819")
                );

        // When & Then - 예외가 발생하지 않아야 함
        result.validateOrThrow();
    }

    @Test
    @DisplayName("LocationValidationResult - validateOrThrow 실패")
    void locationValidationResult_ValidateOrThrow_Fail() {
        // Given
        LocationUtil.LocationValidationResult result = 
                new LocationUtil.LocationValidationResult(
                    false, 
                    0.2, 
                    "테스트 경기장",
                    new BigDecimal("35.8411"),
                    new BigDecimal("128.6819")
                );

        // When & Then
        assertThatThrownBy(result::validateOrThrow)
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("LocationValidationResult - 거리 미터 변환")
    void locationValidationResult_GetDistanceMeters() {
        // Given
        double distanceKm = 0.1; // 0.1km = 100m
        LocationUtil.LocationValidationResult result = 
                new LocationUtil.LocationValidationResult(
                    true, 
                    distanceKm, 
                    "테스트 경기장",
                    new BigDecimal("35.8411"),
                    new BigDecimal("128.6819")
                );

        // When
        double distanceMeters = result.getDistanceMeters();

        // Then
        assertThat(distanceMeters).isEqualTo(100.0);
    }

    @Test
    @DisplayName("LocationValidationResult - of 팩토리 메서드")
    void locationValidationResult_Of() {
        // Given
        Stadium stadium = Stadium.DAEGU;
        boolean withinRange = true;
        double distance = 0.05;

        // When
        LocationUtil.LocationValidationResult result = 
                LocationUtil.LocationValidationResult.of(withinRange, distance, stadium);

        // Then
        assertThat(result.withinRange()).isEqualTo(withinRange);
        assertThat(result.distanceKm()).isEqualTo(distance);
        assertThat(result.stadiumName()).isEqualTo(stadium.getStadiumName());
        assertThat(result.stadiumLatitude()).isEqualTo(stadium.getLatitude());
        assertThat(result.stadiumLongitude()).isEqualTo(stadium.getLongitude());
    }
}