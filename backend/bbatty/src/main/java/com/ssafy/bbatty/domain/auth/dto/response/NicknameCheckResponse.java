package com.ssafy.bbatty.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 닉네임 중복 확인 응답 DTO
 * 
 * 🎯 프론트엔드 개발자를 위한 가이드:
 * - available: 사용 가능 여부 (true: 사용 가능, false: 중복)
 * - message: 사용자에게 표시할 메시지
 */
@Getter
@Builder
public class NicknameCheckResponse {

    /**
     * 닉네임 사용 가능 여부
     * 📝 사용법:
     * - true: 닉네임 사용 가능
     * - false: 닉네임 중복으로 사용 불가
     */
    private final boolean available;

    /**
     * 사용자에게 표시할 메시지
     * 📝 사용법:
     * - UI에서 사용자에게 결과를 안내하는 메시지
     */
    private final String message;

    /**
     * 사용 가능한 닉네임 응답 생성
     */
    public static NicknameCheckResponse available() {
        return NicknameCheckResponse.builder()
                .available(true)
                .message("사용 가능한 닉네임입니다.")
                .build();
    }

    /**
     * 중복된 닉네임 응답 생성
     */
    public static NicknameCheckResponse unavailable() {
        return NicknameCheckResponse.builder()
                .available(false)
                .message("이미 사용 중인 닉네임입니다.")
                .build();
    }
}