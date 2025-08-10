package com.ssafy.chat.match.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 인증 결과를 담는 DTO
 */
@Getter
@Builder
public class AuthResult {
    private final Map<String, Object> userInfo;
    private final Map<String, Object> gameInfo;
    private final String gameIdStr;
    private final String gameDateStr;
    private final Long userId;
}