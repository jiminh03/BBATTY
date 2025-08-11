package com.ssafy.chat.common.domain;

/**
 * 채팅방 상태 열거형
 */
public enum ChatRoomStatus {
    
    /**
     * 활성 상태 - 메시지 송수신 가능
     */
    ACTIVE,
    
    /**
     * 비활성 상태 - 참여자가 없거나 일시적으로 비활성
     */
    INACTIVE,
    
    /**
     * 종료 상태 - 영구적으로 종료됨, 재활성화 불가
     */
    CLOSED
}