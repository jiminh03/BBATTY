package com.ssafy.chat.match.domain;

import com.ssafy.chat.common.util.KSTTimeUtil;
import lombok.Builder;
import lombok.Getter;

/**
 * 매칭 채팅방 도메인 객체 (심플 버전)
 * 핵심 비즈니스 로직만 포함
 */
@Getter
@Builder(toBuilder = true)
public class MatchChatRoom {
    
    private final String matchId;
    private final Long gameId;
    private final String matchTitle;
    private final String matchDescription;
    private final Long teamId;
    private final int minAge;
    private final int maxAge;
    private final String genderCondition;
    private final int maxParticipants;
    private final int currentParticipants;
    private final int minWinRate;
    private final String createdAt;
    private final String lastActivityAt;
    private final MatchChatRoomStatus status;
    private final String ownerId;

    /**
     * 새로운 매칭 채팅방 생성
     */
    public static MatchChatRoom create(String matchId, Long gameId, String matchTitle, 
                                     String matchDescription, Long teamId, int minAge, int maxAge,
                                     String genderCondition, int maxParticipants, int minWinRate, String ownerId) {
        
        return MatchChatRoom.builder()
                .matchId(matchId)
                .gameId(gameId)
                .matchTitle(matchTitle)
                .matchDescription(matchDescription)
                .teamId(teamId)
                .minAge(minAge)
                .maxAge(maxAge)
                .genderCondition(genderCondition)
                .maxParticipants(maxParticipants)
                .minWinRate(minWinRate)
                .currentParticipants(0)
                .createdAt(KSTTimeUtil.nowAsString())
                .lastActivityAt(KSTTimeUtil.nowAsString())
                .status(MatchChatRoomStatus.ACTIVE)
                .ownerId(ownerId)
                .build();
    }

    /**
     * 핵심 비즈니스 규칙들
     */
    public boolean isFull() {
        return currentParticipants >= maxParticipants;
    }

    public boolean isActive() {
        return status == MatchChatRoomStatus.ACTIVE;
    }

    public boolean canJoin() {
        return isActive() && !isFull();
    }

    public boolean isOwner(String userId) {
        return ownerId.equals(userId);
    }
}