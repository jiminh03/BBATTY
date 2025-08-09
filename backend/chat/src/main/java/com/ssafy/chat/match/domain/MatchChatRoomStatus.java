package com.ssafy.chat.match.domain;

/**
 * 매칭 채팅방 상태
 */
public enum MatchChatRoomStatus {
    
    /**
     * 활성화 상태 - 사용자 입장 가능
     */
    ACTIVE("ACTIVE"),
    
    /**
     * 정원 만석 - 더 이상 입장 불가
     */
    FULL("FULL"),
    
    /**
     * 비활성화 - 방장이 방을 닫음
     */
    CLOSED("CLOSED");
    
    private final String value;
    
    MatchChatRoomStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 문자열로부터 enum 변환
     */
    public static MatchChatRoomStatus fromString(String status) {
        for (MatchChatRoomStatus roomStatus : values()) {
            if (roomStatus.value.equals(status)) {
                return roomStatus;
            }
        }
        return ACTIVE; // 기본값
    }
    
    @Override
    public String toString() {
        return value;
    }
}