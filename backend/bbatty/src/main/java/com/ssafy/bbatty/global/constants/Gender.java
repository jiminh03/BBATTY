package com.ssafy.bbatty.global.constants;

import com.ssafy.bbatty.global.exception.ApiException;

/**
 * 성별 Enum 정의
 * 카카오 API 응답값과 매핑을 위한 유틸리티 메서드 포함
 */
public enum Gender {
    MALE("male"),
    FEMALE("female");
    
    private final String kakaoValue;
    
    Gender(String kakaoValue) {
        this.kakaoValue = kakaoValue;
    }
    
    /**
     * 카카오 API 응답값을 Gender enum으로 변환
     * @param kakaoGender 카카오 API에서 받은 성별 값 ("male", "female")
     * @return Gender enum
     * @throws ApiException 유효하지 않은 값이거나 null인 경우
     */
    public static Gender fromKakaoValue(String kakaoGender) {
        if (kakaoGender == null || kakaoGender.trim().isEmpty()) {
            throw new ApiException(ErrorCode.KAKAO_GENDER_INFO_REQUIRED);
        }

        return switch (kakaoGender.toLowerCase().trim()) {
            case "female" -> Gender.FEMALE;
            case "male" -> Gender.MALE;
            default -> throw new ApiException(ErrorCode.KAKAO_GENDER_INFO_INVALID);
        };
    }
    
    /**
     * 카카오 API용 값 반환
     * @return 카카오 API에서 사용하는 성별 값
     */
    public String getKakaoValue() {
        return kakaoValue;
    }
}