package com.ssafy.chat.common.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * 사용자 ID 값 객체
 * 타입 안전성을 제공하고 도메인 의미를 명확화
 */
@Getter
@EqualsAndHashCode
@ToString
public class UserId {
    
    private final Long value;
    
    private UserId(Long value) {
        this.value = Objects.requireNonNull(value, "사용자 ID는 null일 수 없습니다");
        validate(value);
    }
    
    /**
     * Long 값으로부터 UserId 생성
     */
    public static UserId from(Long value) {
        return new UserId(value);
    }
    
    /**
     * ID 유효성 검증
     */
    private void validate(Long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다: " + value);
        }
    }
    
    /**
     * Long 변환
     */
    public Long asLong() {
        return value;
    }
}