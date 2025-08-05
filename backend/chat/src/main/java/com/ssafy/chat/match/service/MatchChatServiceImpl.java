package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.MatchChatMessage;
import com.ssafy.chat.match.kafka.MatchChatKafkaProducer;
import com.ssafy.chat.match.kafka.MatchChatKafkaComsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;


import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 매칭 채팅 서비스 구현체
 * Kafka Producer/Consumer와 연동하여 매칭 채팅 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatServiceImpl implements MatchChatService {
    
    private final MatchChatKafkaProducer kafkaProducer;
    private final MatchChatKafkaComsumer kafkaConsumer;
    
    @Override
    public void addSessionToMatchRoom(String matchId, WebSocketSession session) {
        log.debug("매칭 채팅방에 세션 추가 - matchId: {}, sessionId: {}", matchId, session.getId());
        kafkaConsumer.addSessionToMatchChatRoom(matchId, session);
    }
    
    @Override
    public void removeSessionFromMatchRoom(String matchId, WebSocketSession session) {
        log.debug("매칭 채팅방에서 세션 제거 - matchId: {}, sessionId: {}", matchId, session.getId());
        kafkaConsumer.removeSessioniFromMatchRoom(matchId, session);
    }
    
    @Override
    public void sendChatMessage(String matchId, MatchChatMessage message) {
        log.debug("채팅 메시지 발송 - matchId: {}, userId: {}", matchId, message.getUserId());
        kafkaProducer.sendChatMessage(matchId, message);
    }
    
    @Override
    public void sendUserJoinEvent(String matchId, String userId, String userName) {
        log.debug("사용자 입장 이벤트 발송 - matchId: {}, userId: {}", matchId, userId);
        kafkaProducer.sendUserJoinEvent(matchId, userId, userName);
    }
    
    @Override
    public void sendUserLeaveEvent(String matchId, String userId, String userName) {
        log.debug("사용자 퇴장 이벤트 발송 - matchId: {}, userId: {}", matchId, userId);
        kafkaProducer.sendUserLeaveEvent(matchId, userId, userName);
    }
    
    @Override
    public List<Map<String, Object>> getRecentMessages(String matchId, int limit) {
        log.debug("최근 메시지 조회 - matchId: {}, limit: {}", matchId, limit);
        return kafkaConsumer.getRecentMessages(matchId, limit);
    }
    
    @Override
    public List<Map<String, Object>> getRecentMessages(String matchId, long lastMessageTimestamp, int limit) {
        log.debug("특정 시점 이전 메시지 조회 - matchId: {}, beforeTimestamp: {}, limit: {}", 
                matchId, lastMessageTimestamp, limit);
        return kafkaConsumer.getRecentMessages(matchId, lastMessageTimestamp, limit);
    }
    
    @Override
    public int getActiveMatchRoomCount() {
        return kafkaConsumer.getActiveMatchRoomCount();
    }
    
    @Override
    public int getActiveSessionCount(String matchId) {
        return kafkaConsumer.getActiveSessionCount(matchId);
    }
}