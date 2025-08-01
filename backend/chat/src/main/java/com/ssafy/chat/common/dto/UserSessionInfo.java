package com.ssafy.chat.common.dto;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자 세션 정보 클래스
 */
@Getter
public class UserSessionInfo {
    private final String userId;
    private final String userName;
    private final String roomId;
    private final Map<String, Object> additionalInfo = new ConcurrentHashMap<>();

    public UserSessionInfo(String userId, String userName, String roomId) {
        this.userId = userId;
        this.userName = userName;
        this.roomId = roomId;
    }

    public void addAdditionalInfo(String key, Object value) {
        additionalInfo.put(key, value);
    }
}