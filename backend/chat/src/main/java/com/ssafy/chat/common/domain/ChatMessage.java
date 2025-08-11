package com.ssafy.chat.common.domain;

import com.ssafy.chat.common.enums.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 채팅 메시지 도메인 엔티티
 * 메시지의 핵심 비즈니스 로직과 상태를 관리
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class ChatMessage {
    
    /**
     * 메시지 고유 ID (타임스탬프 기반)
     */
    private final Long messageId;
    
    /**
     * 메시지 타입
     */
    private final MessageType messageType;
    
    /**
     * 발신자 ID
     */
    private final UserId senderId;
    
    /**
     * 채팅방 ID
     */
    private final ChatRoomId roomId;
    
    /**
     * 메시지 내용
     */
    private final String content;
    
    /**
     * 발송 시각
     */
    private final LocalDateTime timestamp;
    
    /**
     * 발신자 닉네임 (표시용)
     */
    private final String senderNickname;
    
    /**
     * 메시지 메타데이터 (이미지 URL, 시스템 메시지 정보 등)
     */
    @Builder.Default
    private final java.util.Map<String, Object> metadata = new java.util.HashMap<>();
    
    // ===========================================
    // 비즈니스 로직 메서드
    // ===========================================
    
    /**
     * 시스템 메시지 여부 확인
     */
    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM || 
               messageType == MessageType.USER_JOIN ||
               messageType == MessageType.USER_LEAVE;
    }
    
    /**
     * 사용자 채팅 메시지 여부 확인
     */
    public boolean isUserChatMessage() {
        return messageType == MessageType.CHAT;
    }
    
    /**
     * 입장/퇴장 이벤트 메시지 여부 확인
     */
    public boolean isUserEvent() {
        return messageType == MessageType.USER_JOIN || 
               messageType == MessageType.USER_LEAVE;
    }
    
    /**
     * 메시지가 특정 사용자로부터 온 것인지 확인
     */
    public boolean isFromUser(UserId userId) {
        return Objects.equals(this.senderId, userId);
    }
    
    /**
     * 메시지가 특정 채팅방에 속하는지 확인
     */
    public boolean belongsToRoom(ChatRoomId roomId) {
        return Objects.equals(this.roomId, roomId);
    }
    
    /**
     * 메타데이터 조회
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 메타데이터 설정한 새로운 메시지 생성
     */
    public ChatMessage withMetadata(String key, Object value) {
        java.util.Map<String, Object> newMetadata = new java.util.HashMap<>(this.metadata);
        newMetadata.put(key, value);
        
        return this.toBuilder()
                .metadata(newMetadata)
                .build();
    }
    
    /**
     * 메시지 내용이 유효한지 검증
     */
    public boolean hasValidContent() {
        if (isSystemMessage()) {
            return content != null; // 시스템 메시지는 내용이 있기만 하면 됨
        } else {
            return content != null && !content.trim().isEmpty();
        }
    }
    
    // ===========================================
    // 팩토리 메서드
    // ===========================================
    
    /**
     * 사용자 채팅 메시지 생성
     */
    public static ChatMessage createUserMessage(
            UserId senderId, 
            ChatRoomId roomId, 
            String content, 
            String senderNickname) {
        
        return ChatMessage.builder()
                .messageId(System.currentTimeMillis())
                .messageType(MessageType.CHAT)
                .senderId(senderId)
                .roomId(roomId)
                .content(content)
                .senderNickname(senderNickname)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 시스템 메시지 생성
     */
    public static ChatMessage createSystemMessage(
            ChatRoomId roomId, 
            String content) {
        
        return ChatMessage.builder()
                .messageId(System.currentTimeMillis())
                .messageType(MessageType.SYSTEM)
                .senderId(null) // 시스템 메시지는 발신자 없음
                .roomId(roomId)
                .content(content)
                .senderNickname("시스템")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 사용자 입장 메시지 생성
     */
    public static ChatMessage createUserJoinMessage(
            UserId userId, 
            ChatRoomId roomId, 
            String nickname) {
        
        return ChatMessage.builder()
                .messageId(System.currentTimeMillis())
                .messageType(MessageType.USER_JOIN)
                .senderId(userId)
                .roomId(roomId)
                .content(nickname + "님이 입장하셨습니다.")
                .senderNickname(nickname)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 사용자 퇴장 메시지 생성
     */
    public static ChatMessage createUserLeaveMessage(
            UserId userId, 
            ChatRoomId roomId, 
            String nickname) {
        
        return ChatMessage.builder()
                .messageId(System.currentTimeMillis())
                .messageType(MessageType.USER_LEAVE)
                .senderId(userId)
                .roomId(roomId)
                .content(nickname + "님이 퇴장하셨습니다.")
                .senderNickname(nickname)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // ===========================================
    // 유효성 검증
    // ===========================================
    
    /**
     * 메시지의 전체적인 유효성 검증
     */
    public boolean isValid() {
        return messageId != null && messageId > 0
               && messageType != null
               && roomId != null
               && hasValidContent()
               && timestamp != null
               && (isSystemMessage() || (senderId != null && senderNickname != null));
    }
}