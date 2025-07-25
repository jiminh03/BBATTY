package com.ssafy.bbatty.domain.chat.common.dto;

import com.ssafy.bbatty.domain.chat.common.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공통 채팅 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** 메시지 타입 */
    private MessageType type;

    /** 사용자 ID */
    private String userId;

    /** 사용자 이름 */
    private String userName;

    /** 방 ID */
    private String roomId;

    /** 메시지 내용 */
    private String content;

    /** 메시지 전송 시간 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 시스템 메시지 생성 헬퍼
     */
    public static ChatMessage createSystemMessage(String roomId, String content) {
        return ChatMessage.builder()
                .type(MessageType.SYSTEM)
                .roomId(roomId)
                .content(content)
                .build();
    }

    /**
     * 사용자 채팅 메시지 생성 헬퍼
     */
    public static ChatMessage createUserMessage(String userId, String userName, String roomId, String content) {
        return ChatMessage.builder()
                .type(MessageType.CHAT)
                .userId(userId)
                .userName(userName)
                .roomId(roomId)
                .content(content)
                .build();
    }

    /**
     * 오류 메시지 생성 헬퍼
     */
    public static ChatMessage createErrorMessage(String roomId, String content) {
        return ChatMessage.builder()
                .type(MessageType.ERROR)
                .roomId(roomId)
                .content(content)
                .build();
    }

    /**
     * 사용자 입장 메시지 생성 헬퍼
     */
    public static ChatMessage createUserJoinMessage(String userId, String userName, String roomId) {
        return ChatMessage.builder()
                .type(MessageType.USER_JOIN)
                .userId(userId)
                .userName(userName)
                .roomId(roomId)
                .content(userName + "님이 입장했습니다.")
                .build();
    }

    /**
     * 사용자 퇴장 메시지 생성 헬퍼
     */
    public static ChatMessage createUserLeaveMessage(String userId, String userName, String roomId) {
        return ChatMessage.builder()
                .type(MessageType.USER_LEAVE)
                .userId(userId)
                .userName(userName)
                .roomId(roomId)
                .content(userName + "님이 퇴장했습니다.")
                .build();
    }

    /**
     * JSON 직렬화를 위한 타입 문자열 반환
     */
    public String getTypeCode() {
        return type != null ? type.getCode() : "unknown";
    }

    /**
     * 메시지가 시스템 메시지인지 확인
     */
    public boolean isSystemMessage() {
        return type != null && type.isSystemMessage();
    }

    /**
     * 메시지가 사용자 액션인지 확인
     */
    public boolean isUserAction() {
        return type != null && type.isUserAction();
    }

    /**
     * 브로드캐스트가 필요한 메시지인지 확인
     */
    public boolean needsBroadcast() {
        return type != null && type.needsBroadcast();
    }

    /**
     * 사용자가 전송한 메시지인지 확인
     */
    public boolean isUserChat() {
        return type == MessageType.CHAT && userId != null;
    }

    /**
     * 메시지 유효성 검사
     */
    public boolean isValid() {
        if (roomId == null || roomId.trim().isEmpty()) {
            return false;
        }

        if (type == null) {
            return false;
        }

        // 사용자 메시지의 경우 userId와 userName 필수
        if (type == MessageType.CHAT) {
            return userId != null && !userId.trim().isEmpty() &&
                    userName != null && !userName.trim().isEmpty() &&
                    content != null && !content.trim().isEmpty();
        }

        // 시스템 메시지의 경우 content만 필수
        if (type.isSystemMessage()) {
            return content != null && !content.trim().isEmpty();
        }

        return true;
    }
}