package com.ssafy.chat.common.domain;

import com.ssafy.chat.common.enums.ChatRoomType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * 채팅방 도메인 엔티티
 * 채팅방의 핵심 비즈니스 로직과 상태를 관리
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class ChatRoom {
    
    /**
     * 채팅방 고유 ID
     */
    private final ChatRoomId id;
    
    /**
     * 채팅방 타입 (MATCH, WATCH)
     */
    private final ChatRoomType type;
    
    /**
     * 게임 ID
     */
    private final Long gameId;
    
    /**
     * 채팅방 상태
     */
    private ChatRoomStatus status;
    
    /**
     * 현재 참여자 목록
     */
    @Builder.Default
    private final Set<UserId> participants = new HashSet<>();
    
    /**
     * 최대 참여자 수 (제한 없으면 -1)
     */
    @Builder.Default
    private final int maxParticipants = -1;
    
    /**
     * 생성 시각
     */
    private final LocalDateTime createdAt;
    
    /**
     * 마지막 활동 시각
     */
    private LocalDateTime lastActivityAt;
    
    /**
     * 총 메시지 수
     */
    @Builder.Default
    private long totalMessageCount = 0L;
    
    /**
     * 채팅방 메타데이터 (확장 가능한 추가 정보)
     */
    @Builder.Default
    private final java.util.Map<String, Object> metadata = new java.util.HashMap<>();
    
    // ===========================================
    // 비즈니스 로직 메서드
    // ===========================================
    
    /**
     * 사용자를 채팅방에 추가
     */
    public ChatRoom addParticipant(UserId userId) {
        validateCanJoin(userId);
        
        Set<UserId> newParticipants = new HashSet<>(this.participants);
        newParticipants.add(userId);
        
        return this.toBuilder()
                .participants(newParticipants)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 사용자를 채팅방에서 제거
     */
    public ChatRoom removeParticipant(UserId userId) {
        Set<UserId> newParticipants = new HashSet<>(this.participants);
        newParticipants.remove(userId);
        
        // 참여자가 모두 없어지면 비활성 상태로 변경
        ChatRoomStatus newStatus = newParticipants.isEmpty() ? 
                ChatRoomStatus.INACTIVE : this.status;
        
        return this.toBuilder()
                .participants(newParticipants)
                .status(newStatus)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 메시지 발송 시 호출되는 메서드
     */
    public ChatRoom onMessageSent() {
        return this.toBuilder()
                .totalMessageCount(this.totalMessageCount + 1)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 채팅방 활성화
     */
    public ChatRoom activate() {
        if (status == ChatRoomStatus.CLOSED) {
            throw new IllegalStateException("종료된 채팅방은 다시 활성화할 수 없습니다: " + id.getValue());
        }
        
        return this.toBuilder()
                .status(ChatRoomStatus.ACTIVE)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 채팅방 비활성화
     */
    public ChatRoom deactivate() {
        return this.toBuilder()
                .status(ChatRoomStatus.INACTIVE)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 채팅방 종료 (영구 종료)
     */
    public ChatRoom close() {
        return this.toBuilder()
                .status(ChatRoomStatus.CLOSED)
                .participants(Collections.emptySet()) // 모든 참여자 제거
                .lastActivityAt(LocalDateTime.now())
                .build();
    }
    
    // ===========================================
    // 조회 및 검증 메서드
    // ===========================================
    
    /**
     * 현재 참여자 수
     */
    public int getCurrentParticipantCount() {
        return participants.size();
    }
    
    /**
     * 참여자 목록 (읽기 전용)
     */
    public Set<UserId> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }
    
    /**
     * 특정 사용자가 참여 중인지 확인
     */
    public boolean hasParticipant(UserId userId) {
        return participants.contains(userId);
    }
    
    /**
     * 채팅방이 비어있는지 확인
     */
    public boolean isEmpty() {
        return participants.isEmpty();
    }
    
    /**
     * 채팅방이 가득 찼는지 확인
     */
    public boolean isFull() {
        return maxParticipants > 0 && participants.size() >= maxParticipants;
    }
    
    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return status == ChatRoomStatus.ACTIVE;
    }
    
    /**
     * 종료 상태인지 확인
     */
    public boolean isClosed() {
        return status == ChatRoomStatus.CLOSED;
    }
    
    /**
     * 매칭 채팅방인지 확인
     */
    public boolean isMatchChatRoom() {
        return type == ChatRoomType.MATCH;
    }
    
    /**
     * 관전 채팅방인지 확인
     */
    public boolean isWatchChatRoom() {
        return type == ChatRoomType.WATCH;
    }
    
    /**
     * 메타데이터 조회
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 메타데이터 설정
     */
    public ChatRoom setMetadata(String key, Object value) {
        java.util.Map<String, Object> newMetadata = new java.util.HashMap<>(this.metadata);
        newMetadata.put(key, value);
        
        return this.toBuilder()
                .metadata(newMetadata)
                .build();
    }
    
    // ===========================================
    // 내부 검증 메서드
    // ===========================================
    
    /**
     * 사용자가 채팅방에 참여할 수 있는지 검증
     */
    private void validateCanJoin(UserId userId) {
        if (status == ChatRoomStatus.CLOSED) {
            throw new IllegalStateException("종료된 채팅방에는 참여할 수 없습니다: " + id.getValue());
        }
        
        if (participants.contains(userId)) {
            throw new IllegalStateException("이미 채팅방에 참여 중입니다: " + userId.getValue());
        }
        
        if (isFull()) {
            throw new IllegalStateException("채팅방이 가득 찼습니다: " + id.getValue());
        }
    }
    
    // ===========================================
    // 팩토리 메서드
    // ===========================================
    
    /**
     * 매칭 채팅방 생성
     */
    public static ChatRoom createMatchChatRoom(String matchId, Long gameId) {
        return ChatRoom.builder()
                .id(ChatRoomId.forMatch(matchId))
                .type(ChatRoomType.MATCH)
                .gameId(gameId)
                .status(ChatRoomStatus.ACTIVE)
                .maxParticipants(-1) // 매칭 채팅방은 제한 없음
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 관전 채팅방 생성
     */
    public static ChatRoom createWatchChatRoom(Long gameId, Long teamId) {
        return ChatRoom.builder()
                .id(ChatRoomId.forWatch(gameId, teamId))
                .type(ChatRoomType.WATCH)
                .gameId(gameId)
                .status(ChatRoomStatus.ACTIVE)
                .maxParticipants(-1) // 관전 채팅방도 제한 없음
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build()
                .setMetadata("teamId", teamId);
    }
}