package com.ssafy.bbatty.domain.attendance.dto;

import com.ssafy.bbatty.domain.attendance.dto.request.AttendanceVerifyRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceVerifyRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("유효한 좌표 검증 - 성공")
    void isValidCoordinates_Success() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("유효한 좌표 검증 - null 위도")
    void isValidCoordinates_NullLatitude() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(null)
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("유효한 좌표 검증 - null 경도")
    void isValidCoordinates_NullLongitude() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(null)
                .build();

        // When
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("유효한 좌표 검증 - 위도 범위 초과")
    void isValidCoordinates_LatitudeOutOfRange() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("91.0")) // 90도 초과
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("유효한 좌표 검증 - 경도 범위 초과")
    void isValidCoordinates_LongitudeOutOfRange() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("181.0")) // 180도 초과
                .build();

        // When
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Bean Validation - 유효한 요청")
    void beanValidation_ValidRequest() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Bean Validation - null 위도")
    void beanValidation_NullLatitude() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(null)
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("위도는 필수입니다");
    }

    @Test
    @DisplayName("Bean Validation - null 경도")
    void beanValidation_NullLongitude() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(null)
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("경도는 필수입니다");
    }

    @Test
    @DisplayName("Bean Validation - 위도 최소값 위반")
    void beanValidation_LatitudeBelowMin() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("-91.0"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("위도는 -90도 이상이어야 합니다");
    }

    @Test
    @DisplayName("Bean Validation - 위도 최대값 위반")
    void beanValidation_LatitudeAboveMax() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("91.0"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("위도는 90도 이하여야 합니다");
    }

    @Test
    @DisplayName("Bean Validation - 경도 최소값 위반")
    void beanValidation_LongitudeBelowMin() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("-181.0"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("경도는 -180도 이상이어야 합니다");
    }

    @Test
    @DisplayName("Bean Validation - 경도 최대값 위반")
    void beanValidation_LongitudeAboveMax() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("181.0"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("경도는 180도 이하여야 합니다");
    }

    @Test
    @DisplayName("경계값 테스트 - 위도 최소값")
    void boundaryTest_LatitudeMin() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("-90.0"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(violations).isEmpty();
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("경계값 테스트 - 위도 최대값")
    void boundaryTest_LatitudeMax() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("90.0"))
                .longitude(new BigDecimal("128.6819"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(violations).isEmpty();
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("경계값 테스트 - 경도 최소값")
    void boundaryTest_LongitudeMin() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("-180.0"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(violations).isEmpty();
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("경계값 테스트 - 경도 최대값")
    void boundaryTest_LongitudeMax() {
        // Given
        AttendanceVerifyRequest request = AttendanceVerifyRequest.builder()
                .latitude(new BigDecimal("35.8411"))
                .longitude(new BigDecimal("180.0"))
                .build();

        // When
        Set<ConstraintViolation<AttendanceVerifyRequest>> violations = validator.validate(request);
        boolean isValid = request.isValidCoordinates();

        // Then
        assertThat(violations).isEmpty();
        assertThat(isValid).isTrue();
    }
}