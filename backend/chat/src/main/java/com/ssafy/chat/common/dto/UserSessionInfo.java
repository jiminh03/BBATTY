package com.ssafy.chat.common.dto;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자 세션 정보 클래스
 */
@Getter
public class UserSessionInfo {
    private final Long userId;
    private final String nickname;
    private final String roomId;
    private final Map<String, Object> additionalInfo = new ConcurrentHashMap<>();

    public UserSessionInfo(Long userId, String nickname, String roomId) {
        this.userId = userId;
        this.nickname = nickname;
        this.roomId = roomId;
    }

    public void addAdditionalInfo(String key, Object value) {
        additionalInfo.put(key, value);
    }
}