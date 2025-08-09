package com.ssafy.bbatty.domain.chat.common.dto;

import com.ssafy.bbatty.domain.chat.common.enums.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 채팅방 상태 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomStatus {

    /** 방 ID */
    private String roomId;


    /** 방 닉네임 */
    private String roomName;

    /** 채팅방 타입 */
    private ChatRoomType roomType;

    /** 활성화 상태 */
    private boolean active;

    /** 현재 접속자 수 */
    private Long connectedUserCount;

    /** 최대 접속자 수 */
    private Integer maxUserCount;

    /** 접속한 사용자 ID 목록 */
    private Set<String> connectedUsers;

    /** 방 생성 시간 */
    private LocalDateTime createdAt;

    /** 마지막 활동 시간 */
    private LocalDateTime lastActivity;

    @Builder.Default
    private LocalDateTime statusTime = LocalDateTime.now();

    /**
     * 채팅방이 가득 찼는지 확인
     */
    public boolean isFull() {
        return maxUserCount != null &&
                connectedUserCount != null &&
                connectedUserCount >= maxUserCount;
    }

    /**
     * 채팅방이 비어있는지 확인
     */
    public boolean isEmpty() {
        return connectedUserCount == null || connectedUserCount == 0;
    }

    /**
     * 특정 사용자가 접속 중인지 확인
     */
    public boolean isUserConnected(String userId) {
        return connectedUsers != null && connectedUsers.contains(userId);
    }

    /**
     * 채팅방 사용률 계산 (%)
     */
    public double getUsageRate() {
        if (maxUserCount == null || maxUserCount == 0 || connectedUserCount == null) {
            return 0.0;
        }
        return (double) connectedUserCount / maxUserCount * 100.0;
    }

    /**
     * 게임 채팅방인지 확인
     */
    public boolean isGameRoom() {
        return roomType == ChatRoomType.GAME;

    /**
     * 매칭 채팅방인지 확인
     */
    public boolean isMatchRoom() {
        return roomType == ChatRoomType.MATCH;
    }
}
