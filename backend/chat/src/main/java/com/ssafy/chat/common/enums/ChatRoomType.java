package com.ssafy.chat.common.enums;

import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 채팅방 타입
 */
@Getter
public enum ChatRoomType {

    WATCH("watch", "관전 채팅"),
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
            throw new ApiException(ErrorCode.BAD_REQUEST, "알 수 없는 채팅방 타입 코드: " + code);
        }
        return type;
    }
}
