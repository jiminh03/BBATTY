package com.ssafy.bbatty.domain.chat.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.service.ChatAuthService;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 채팅 인증 요청을 수신하는 Kafka Consumer
 * @deprecated 이제 MatchChatRequestConsumer와 WatchChatRequestConsumer를 사용합니다.
 */
@Deprecated
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAuthRequestConsumer {
    
    private final ObjectMapper objectMapper;
    private final ChatAuthService chatAuthService;
    private final JwtProvider jwtProvider;
    
    @KafkaListener(topics = "chat-auth-request", groupId = "bbatty-auth-service")
    public void handleAuthRequest(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String requestId) {
        
        try {
            log.debug("채팅 인증 요청 수신: requestId={}", requestId);
            
            // JSON 메시지 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> authRequestData = objectMapper.readValue(message, Map.class);
            
            // JWT 토큰에서 사용자 정보 추출
            String jwtToken = (String) authRequestData.get("jwtToken");
            Long userId = jwtProvider.getUserId(jwtToken);
            Long userTeamId = jwtProvider.getTeamId(jwtToken);
            String userGender = jwtProvider.getGender(jwtToken);
            int userAge = jwtProvider.getAge(jwtToken);
            
            // 채팅 유형에 따라 nickname 처리 (MATCH: 클라이언트 제공, WATCH: 익명)
            String chatType = (String) authRequestData.get("chatType");
            String userNickname = null;
            if ("MATCH".equals(chatType)) {
                userNickname = (String) authRequestData.get("nickname");
            } else if ("WATCH".equals(chatType)) {
                userNickname = null; // 익명 채팅
            }
            
            // ChatAuthRequest DTO 생성  
            Object gameIdObj = authRequestData.get("gameId");
            Long matchId = (gameIdObj instanceof Integer) ? ((Integer) gameIdObj).longValue() : (Long) gameIdObj;
            
            // teamId 추출 (roomInfo에서 가져오거나 직접 전달된 값 사용)
            @SuppressWarnings("unchecked")
            Map<String, Object> roomInfo = (Map<String, Object>) authRequestData.get("roomInfo");
            Object teamIdObj = roomInfo != null ? roomInfo.get("teamId") : null;
            Long teamId = teamIdObj != null ? 
                ((teamIdObj instanceof Integer) ? ((Integer) teamIdObj).longValue() : (Long) teamIdObj) : null;
            
            ChatAuthRequest chatAuthRequest = ChatAuthRequest.builder()
                    .requestId(requestId)
                    .chatType((String) authRequestData.get("chatType"))
                    .action((String) authRequestData.get("action"))
                    .matchId(matchId)
                    .teamId(teamId)
                    .roomInfo(roomInfo)
                    .build();
            
            // 인증 서비스 호출 (JWT에서 추출한 모든 정보 전달)
            chatAuthService.authorizeChatAccess(userId, userTeamId, userGender, userAge, userNickname, chatAuthRequest);
            
            log.info("채팅 인증 요청 처리 완료: requestId={}", requestId);
            
        } catch (Exception e) {
            log.error("채팅 인증 요청 처리 실패: requestId={}", requestId, e);
        }
    }
}