package com.ssafy.chat.common.enums;

import lombok.Getter;

@Getter
public enum MessageType {
    /** 일반 채팅 메시지 */
    CHAT("chat", "채팅 메시지"),

    /** 시스템 메시지 */
    SYSTEM("system", "시스템 메시지"),

    /** 오류 메시지 */
    ERROR("error", "오류 메시지"),

    /** 사용자 입장 */
    USER_JOIN("user_join", "사용자 입장"),

    /** 사용자 퇴장 */
    USER_LEAVE("user_leave", "사용자 퇴장"),

    /** 사용자 활동 (하트비트) */
    USER_ACTIVITY("user_activity", "사용자 활동");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 코드로 MessageType 찾기
     *
     */
    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type code: " + code);
    }

    /**
     * 시스템 관련 메시지인지 확인
     */
    public boolean isSystemMessage() {
        return this == SYSTEM || this == ERROR;
    }

    /**
     * 사용자 액션 메시지인지 확인
     */
    public boolean isUserAction() {
        return this == USER_JOIN || this == USER_LEAVE || this == USER_ACTIVITY;
    }
}
