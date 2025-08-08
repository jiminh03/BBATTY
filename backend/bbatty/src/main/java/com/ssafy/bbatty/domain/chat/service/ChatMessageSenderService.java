package com.ssafy.bbatty.domain.chat.service;

import com.ssafy.bbatty.domain.chat.dto.response.ChatAuthResponse;
import com.ssafy.bbatty.domain.chat.kafka.ChatAuthKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 채팅 메시지 전송 전담 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageSenderService {
    
    private final ChatAuthKafkaProducer chatAuthKafkaProducer;
    
    /**
     * Kafka로 인증 성공 결과 전송
     */
    public void sendAuthSuccessToKafka(String requestId, ChatAuthResponse.UserInfo userInfo, 
                                      ChatAuthResponse.ChatRoomInfo chatRoomInfo, Map<String, Object> gameInfo) {
        Map<String, Object> authResult;
        
        if (gameInfo != null) {
            // 매칭 채팅인 경우 gameInfo 포함
            authResult = Map.of(
                    "success", true,
                    "requestId", requestId,
                    "timestamp", LocalDateTime.now().toString(),
                    "userInfo", userInfo,
                    "chatRoomInfo", chatRoomInfo,
                    "gameInfo", gameInfo
            );
        } else {
            // 관전 채팅인 경우 기존과 동일
            authResult = Map.of(
                    "success", true,
                    "requestId", requestId,
                    "timestamp", LocalDateTime.now().toString(),
                    "userInfo", userInfo,
                    "chatRoomInfo", chatRoomInfo
            );
        }
        
        chatAuthKafkaProducer.sendAuthResult(requestId, authResult);
        
        if (gameInfo != null) {
            log.info("매칭 채팅 인증 응답 전송 - requestId: {}, gameId: {}, gameDate: {}", 
                    requestId, gameInfo.get("gameId"), gameInfo.get("gameDate"));
        }
    }
    
    /**
     * Kafka로 인증 실패 결과 전송
     */
    public void sendAuthFailureToKafka(String requestId, String errorMessage) {
        Map<String, Object> authResult = Map.of(
                "success", false,
                "requestId", requestId,
                "timestamp", LocalDateTime.now().toString(),
                "errorMessage", errorMessage
        );
        
        chatAuthKafkaProducer.sendAuthResult(requestId, authResult);
    }
}