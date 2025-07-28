package com.ssafy.bbatty.domain.chat.common.enums;

/**
 * 채팅방 타입
 */
public enum ChatRoomType {

    /** 게임 채팅 */
    GAME("game", "게임 채팅"),

    /** 매칭 채팅 */
    MATCH("match", "매칭 채팅");

    private final String code;
    private final String description;

    ChatRoomType(String code, String description) {
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
     * 코드로 ChatRoomType 찾기
     */
    public static ChatRoomType fromCode(String code) {
        for (ChatRoomType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown chat room type code: " + code);
    }

    /**
     * 게임 채팅인지 확인
     */
    public boolean isGameChat() {
        return this == GAME;
    }

    /**
     * 매칭 채팅인지 확인
     */
    public boolean isMatchChat() {
        return this == MATCH;
    }
}
