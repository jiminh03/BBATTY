package com.ssafy.chat.common.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * 채팅방 ID 값 객체
 * 타입 안전성을 제공하고 도메인 의미를 명확화
 */
@Getter
@EqualsAndHashCode
@ToString
public class ChatRoomId {
    
    private final String value;
    
    private ChatRoomId(String value) {
        this.value = Objects.requireNonNull(value, "채팅방 ID는 null일 수 없습니다");
        validate(value);
    }
    
    /**
     * 매칭 채팅방 ID 생성
     */
    public static ChatRoomId forMatch(String matchId) {
        return new ChatRoomId(matchId);
    }
    
    /**
     * 관전 채팅방 ID 생성
     */
    public static ChatRoomId forWatch(Long gameId, Long teamId) {
        String watchRoomId = "watch_" + gameId + "_" + teamId;
        return new ChatRoomId(watchRoomId);
    }
    
    /**
     * 문자열로부터 채팅방 ID 생성
     */
    public static ChatRoomId from(String value) {
        return new ChatRoomId(value);
    }
    
    /**
     * 매칭 채팅방 여부 확인
     */
    public boolean isMatchChatRoom() {
        return value.startsWith("match_");
    }
    
    /**
     * 관전 채팅방 여부 확인
     */
    public boolean isWatchChatRoom() {
        return value.startsWith("watch_");
    }
    
    /**
     * 게임 ID 추출 (매칭/관전 공통)
     */
    public Long extractGameId() {
        if (isMatchChatRoom()) {
            // match_1_67271f38 형태에서 gameId(1) 추출
            String[] parts = value.split("_");
            if (parts.length >= 2) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("매칭 채팅방 ID에서 게임 ID 추출 실패: " + value);
                }
            }
        } else if (isWatchChatRoom()) {
            // watch_1_2 형태에서 gameId(1) 추출
            String[] parts = value.split("_");
            if (parts.length >= 2) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("관전 채팅방 ID에서 게임 ID 추출 실패: " + value);
                }
            }
        }
        throw new IllegalStateException("채팅방 ID 형식이 올바르지 않습니다: " + value);
    }
    
    /**
     * 팀 ID 추출 (관전 채팅방만)
     */
    public Long extractTeamId() {
        if (!isWatchChatRoom()) {
            throw new IllegalStateException("관전 채팅방이 아닙니다: " + value);
        }
        
        // watch_1_2 형태에서 teamId(2) 추출
        String[] parts = value.split("_");
        if (parts.length >= 3) {
            try {
                return Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("관전 채팅방 ID에서 팀 ID 추출 실패: " + value);
            }
        }
        throw new IllegalStateException("관전 채팅방 ID 형식이 올바르지 않습니다: " + value);
    }
    
    /**
     * ID 유효성 검증
     */
    private void validate(String value) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅방 ID는 빈 값일 수 없습니다");
        }
        
        if (!value.matches("^(match_|watch_)[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("채팅방 ID 형식이 올바르지 않습니다: " + value);
        }
    }
    
    /**
     * 문자열 변환
     */
    public String asString() {
        return value;
    }
}