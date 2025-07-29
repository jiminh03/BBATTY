package com.ssafy.bbatty.domain.chat.common.enums;

/**
 * 사용자 상태
 */
public enum UserStatus {

    /** 활성 상태 */
    ACTIVE("active", "활성"),

    /** 비활성 상태 */
    INACTIVE("inactive", "비활성"),

    /** 차단됨 */
    BLOCKED("blocked", "차단됨");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 코드로 UserStatus 찾기
     */
    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status code: " + code);
    }

    /**
     * 메시지 전송 가능한 상태인지 확인
     */
    public boolean canSendMessage() {
        return this == ACTIVE;
    }

    /**
     * 알림 수신 가능한 상태인지 확인
     */
    public boolean canReceiveNotification() {
        return this == ACTIVE;
    }

    /**
     * 채팅 참여 가능한 상태인지 확인
     */
    public boolean canParticipateInChat() {
        return this != BLOCKED;
    }
}
