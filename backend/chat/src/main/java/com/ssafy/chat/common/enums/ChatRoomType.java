package com.ssafy.chat.common.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 채팅방 타입
 */
@Getter
public enum ChatRoomType {

    GAME("game", "게임 채팅"),
    MATCH("match", "매칭 채팅");

    private final String code;
    private final String description;

    // 코드값으로 enum을 빠르게 찾기 위한 Map 캐싱
    private static final Map<String, ChatRoomType> BY_CODE = new HashMap<>();

    static {
        for (ChatRoomType type : values()) {
            BY_CODE.put(type.code, type);
        }
    }

    ChatRoomType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ChatRoomType fromCode(String code) {
        ChatRoomType type = BY_CODE.get(code);
        if (type == null) {
            throw new IllegalArgumentException("Unknown chat room type code: " + code);
        }
        return type;
    }
}
