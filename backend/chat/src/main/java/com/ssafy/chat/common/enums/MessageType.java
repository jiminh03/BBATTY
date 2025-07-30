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
    USER_ACTIVITY("user_activity", "사용자 활동"),

    /** 매칭 상태 업데이트 */
    MATCH_STATUS("match_status", "매칭 상태"),

    /** 매칭 완료 */
    MATCH_COMPLETE("match_complete", "매칭 완료"),

    /** 매칭 취소 */
    MATCH_CANCEL("match_cancel", "매칭 취소"),

    /** 게임 시작 */
    GAME_START("game_start", "게임 시작"),

    /** 게임 종료 */
    GAME_END("game_end", "게임 종료"),

    /** 관리자 공지 */
    ADMIN_NOTICE("admin_notice", "관리자 공지"),

    /** 이모티콘/리액션 */
    REACTION("reaction", "이모티콘/리액션");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 코드로 MessageType 찾기
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
        return this == SYSTEM || this == ERROR || this == ADMIN_NOTICE;
    }

    /**
     * 사용자 액션 메시지인지 확인
     */
    public boolean isUserAction() {
        return this == USER_JOIN || this == USER_LEAVE || this == USER_ACTIVITY;
    }

    /**
     * 매칭 관련 메시지인지 확인
     */
    public boolean isMatchRelated() {
        return this == MATCH_STATUS || this == MATCH_COMPLETE || this == MATCH_CANCEL;
    }

    /**
     * 게임 관련 메시지인지 확인
     */
    public boolean isGameRelated() {
        return this == GAME_START || this == GAME_END;
    }

    /**
     * 브로드캐스트가 필요한 메시지인지 확인
     */
    public boolean needsBroadcast() {
        return this != ERROR && this != USER_ACTIVITY;
    }
}
