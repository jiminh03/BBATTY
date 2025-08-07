package com.ssafy.bbatty.domain.chat.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.dto.response.ChatAuthResponse;
import com.ssafy.bbatty.domain.chat.service.ChatAuthService;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.GameStatus;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Match 채팅 관련 요청을 처리하는 Kafka Consumer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatRequestConsumer {
    
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final ChatAuthService chatAuthService;
    private final GameRepository gameRepository;
    private final ChatAuthKafkaProducer chatAuthKafkaProducer;
    
    @KafkaListener(topics = "match-chat-request", groupId = "bbatty-match-chat-group")
    public void handleMatchChatRequest(String message) {
        String requestId = null;
        
        try {
            JsonNode requestNode = objectMapper.readTree(message);
            requestId = requestNode.get("requestId").asText();
            String jwtToken = requestNode.get("jwtToken").asText();
            String action = requestNode.get("action").asText();
            Long gameId = requestNode.get("gameId").asLong();
            
            log.info("Match 채팅 요청 수신: requestId={}, action={}, gameId={}", 
                    requestId, action, gameId);
            
            // JWT 토큰에서 사용자 정보 추출
            var claims = jwtProvider.extractAllClaims(jwtToken);
            Long userId = claims.get("userId", Long.class);
            Long userTeamId = claims.get("teamId", Long.class);
            String userGender = claims.get("gender", String.class);
            Integer userAge = claims.get("age", Integer.class);
            String userNickname = claims.get("nickname", String.class);
            
            if ("CREATE".equals(action)) {
                handleMatchChatRoomCreate(requestId, userId, userTeamId, userGender, userAge, userNickname, requestNode);
            } else if ("JOIN".equals(action)) {
                handleMatchChatRoomJoin(requestId, userId, userTeamId, userGender, userAge, userNickname, requestNode);
            } else {
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
            }
            
        } catch (ApiException e) {
            log.warn("Match 채팅 요청 처리 실패 - ApiException: requestId={}, error={}", requestId, e.getMessage());
            sendErrorResponse(requestId, e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error("Match 채팅 요청 처리 실패 - Exception: requestId={}", requestId, e);
            sendErrorResponse(requestId, "서버 오류가 발생했습니다.");
        }
    }
    
    /**
     * Match 채팅방 생성 요청 처리
     */
    private void handleMatchChatRoomCreate(String requestId, Long userId, Long userTeamId, 
                                         String userGender, Integer userAge, String userNickname, JsonNode requestNode) {
        try {
            Long gameId = requestNode.get("gameId").asLong();
            JsonNode roomCreateInfo = requestNode.get("roomCreateInfo");
            
            // 방 생성 조건 검증 (방장의 조건이 유효한지)
            validateMatchRoomCreation(userId, userTeamId, userGender, userAge, roomCreateInfo, gameId);
            
            // 성공 시 응답 생성
            ChatAuthResponse.ChatRoomInfo chatRoomInfo = ChatAuthResponse.ChatRoomInfo.builder()
                    .roomId(null) // chat 서버에서 생성
                    .chatType("MATCH")
                    .matchId(gameId)
                    .roomName(roomCreateInfo.get("matchTitle").asText())
                    .isNewRoom(true)
                    .build();
            
            ChatAuthResponse.UserInfo userInfo = ChatAuthResponse.UserInfo.builder()
                    .userId(userId)
                    .nickname(userNickname)
                    .teamId(userTeamId)
                    .age(userAge)
                    .gender(userGender)
                    .build();
            
            sendSuccessResponse(requestId, userInfo, chatRoomInfo, roomCreateInfo);
            
            log.info("Match 채팅방 생성 검증 완료: requestId={}, userId={}, gameId={}", 
                    requestId, userId, gameId);
            
        } catch (ApiException e) {
            log.warn("Match 채팅방 생성 검증 실패: requestId={}, error={}", requestId, e.getMessage());
            sendErrorResponse(requestId, e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error("Match 채팅방 생성 처리 중 오류: requestId={}", requestId, e);
            sendErrorResponse(requestId, "서버 오류가 발생했습니다.");
        }
    }
    
    /**
     * Match 채팅방 입장 요청 처리
     */
    private void handleMatchChatRoomJoin(String requestId, Long userId, Long userTeamId, 
                                       String userGender, Integer userAge, String userNickname, JsonNode requestNode) {
        try {
            // 기존 ChatAuthService 로직 사용
            ChatAuthRequest chatAuthRequest = createChatAuthRequestFromNode(requestNode);
            var response = chatAuthService.authorizeChatAccess(userId, userTeamId, userGender, userAge, userNickname, chatAuthRequest);
            
            log.info("Match 채팅방 입장 처리 완료: requestId={}, success={}", requestId, response.isSuccess());
            
        } catch (ApiException e) {
            log.warn("Match 채팅방 입장 처리 실패: requestId={}, error={}", requestId, e.getMessage());
            sendErrorResponse(requestId, e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error("Match 채팅방 입장 처리 중 오류: requestId={}", requestId, e);
            sendErrorResponse(requestId, "서버 오류가 발생했습니다.");
        }
    }
    
    /**
     * Match 채팅방 생성 조건 검증
     */
    private void validateMatchRoomCreation(Long userId, Long userTeamId, String userGender, Integer userAge, 
                                          JsonNode roomCreateInfo, Long gameId) {
        // 1. 경기 존재 및 상태 확인
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
        
        if (game.getStatus() == GameStatus.FINISHED) {
            throw new ApiException(ErrorCode.GAME_FINISHED);
        }
        
        // 2. 방장이 설정한 조건이 자신도 만족하는지 확인
        if (roomCreateInfo != null && !roomCreateInfo.isNull()) {
            // 나이 조건 확인
            if (roomCreateInfo.has("minAge") && roomCreateInfo.has("maxAge")) {
                int minAge = roomCreateInfo.get("minAge").asInt();
                int maxAge = roomCreateInfo.get("maxAge").asInt();
                if (userAge < minAge || userAge > maxAge) {
                    log.warn("방장이 나이 조건을 만족하지 않음: userId={}, age={}, range=[{}-{}]", 
                            userId, userAge, minAge, maxAge);
                    throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
                }
            }
            
            // 성별 조건 확인
            if (roomCreateInfo.has("genderCondition")) {
                String genderCondition = roomCreateInfo.get("genderCondition").asText();
                if (!"ALL".equals(genderCondition) && !userGender.equals(genderCondition)) {
                    log.warn("방장이 성별 조건을 만족하지 않음: userId={}, gender={}, condition={}", 
                            userId, userGender, genderCondition);
                    throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
                }
            }
            
            // 팀 조건 확인
            if (roomCreateInfo.has("teamId")) {
                Long teamCondition = roomCreateInfo.get("teamId").asLong();
                if (!userTeamId.equals(teamCondition)) {
                    log.warn("방장이 팀 조건을 만족하지 않음: userId={}, teamId={}, condition={}", 
                            userId, userTeamId, teamCondition);
                    throw new ApiException(ErrorCode.UNAUTHORIZED_TEAM_ACCESS);
                }
            }
        }
        
        log.info("Match 채팅방 생성 조건 검증 성공: userId={}, gameId={}", userId, gameId);
    }
    
    /**
     * 성공 응답 전송
     */
    private void sendSuccessResponse(String requestId, ChatAuthResponse.UserInfo userInfo, 
                                   ChatAuthResponse.ChatRoomInfo chatRoomInfo, JsonNode roomCreateInfo) {
        try {
            Map<String, Object> authResult = new HashMap<>();
            authResult.put("success", true);
            authResult.put("requestId", requestId);
            authResult.put("timestamp", LocalDateTime.now().toString());
            authResult.put("userInfo", userInfo);
            authResult.put("chatRoomInfo", chatRoomInfo);
            
            // 방 생성 시 추가 정보
            if (roomCreateInfo != null) {
                authResult.put("roomCreateInfo", roomCreateInfo);
            }
            
            chatAuthKafkaProducer.sendAuthResult(requestId, authResult);
            
        } catch (Exception e) {
            log.error("성공 응답 전송 실패: requestId={}", requestId, e);
        }
    }
    
    /**
     * 실패 응답 전송
     */
    private void sendErrorResponse(String requestId, String errorMessage) {
        try {
            Map<String, Object> authResult = new HashMap<>();
            authResult.put("success", false);
            authResult.put("requestId", requestId);
            authResult.put("timestamp", LocalDateTime.now().toString());
            authResult.put("errorMessage", errorMessage);
            
            chatAuthKafkaProducer.sendAuthResult(requestId, authResult);
            
        } catch (Exception e) {
            log.error("실패 응답 전송 실패: requestId={}", requestId, e);
        }
    }
    
    /**
     * JsonNode를 ChatAuthRequest로 변환
     */
    private ChatAuthRequest createChatAuthRequestFromNode(JsonNode node) {
        try {
            return ChatAuthRequest.builder()
                    .requestId(node.get("requestId").asText())
                    .chatType(node.get("chatType").asText())
                    .action(node.get("action").asText())
                    .matchId(node.get("gameId").asLong())
                    .roomInfo(objectMapper.convertValue(node.get("roomInfo"), Map.class))
                    .build();
        } catch (Exception e) {
            log.error("ChatAuthRequest 변환 실패", e);
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}