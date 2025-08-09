package com.ssafy.bbatty.domain.chat.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.dto.response.ChatAuthResponse;
import com.ssafy.bbatty.domain.chat.service.ChatAuthService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 채팅 요청 처리 공통 기능을 제공하는 추상 Consumer 클래스
 * JWT 추출, 예외 처리, 응답 전송 등의 중복 로직을 집중 관리
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractChatRequestConsumer {

    protected final ObjectMapper objectMapper;
    protected final JwtProvider jwtProvider;
    protected final ChatAuthService chatAuthService;
    protected final ChatAuthKafkaProducer chatAuthKafkaProducer;

    /**
     * 공통 메시지 처리 템플릿 메서드
     */
    protected void handleChatRequest(String message, String authHeader, String chatType) {
        String requestId = null;

        try {
            // 1. JSON 파싱 및 requestId 추출
            JsonNode requestNode = objectMapper.readTree(message);
            requestId = extractRequestId(requestNode);

            // 2. JWT 토큰 추출 및 사용자 정보 추출
            String jwtToken = extractJwtToken(authHeader);
            Long userId = getUserIdFromToken(jwtToken);
            Long userTeamId = getTeamIdFromToken(jwtToken);
            String userGender = getGenderFromToken(jwtToken);
            int userAge = getAgeFromToken(jwtToken);
            String userNickname = null; // 매칭/관전 채팅에 따라 다르게 처리

            // 3. 채팅 요청 객체 생성
            ChatAuthRequest chatAuthRequest = createChatAuthRequest(requestNode, requestId, chatType);

            // 4. 채팅 인증 처리 (템플릿 메서드 패턴)
            ApiResponse<ChatAuthResponse> response = chatAuthService.authorizeChatAccess(
                    userId,
                    userTeamId,
                    userGender,
                    userAge,
                    userNickname,
                    chatAuthRequest
            );

            log.info("{} 채팅 인증 처리 완료: requestId={}, userId={}",
                    chatType, requestId, userId);

        } catch (ApiException e) {
            log.warn("{} 채팅 인증 실패: requestId={}, error={}", chatType, requestId, e.getMessage());
            sendErrorResponse(requestId, e.getMessage());

        } catch (Exception e) {
            log.error("{} 채팅 요청 처리 중 예상치 못한 오류: requestId={}", chatType, requestId, e);
            sendErrorResponse(requestId, "서버 내부 오류가 발생했습니다");
        }
    }

    /**
     * 요청 ID 추출
     */
    private String extractRequestId(JsonNode requestNode) {
        JsonNode requestIdNode = requestNode.get("requestId");
        if (requestIdNode == null || requestIdNode.isNull()) {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
        return requestIdNode.asText();
    }

    /**
     * JWT 토큰 추출
     */
    private String extractJwtToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return authHeader.substring(7);
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long getUserIdFromToken(String jwtToken) {
        if (!jwtProvider.validateAccessToken(jwtToken)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
        return jwtProvider.getUserId(jwtToken);
    }
    
    /**
     * JWT 토큰에서 팀 ID 추출
     */
    private Long getTeamIdFromToken(String jwtToken) {
        return jwtProvider.getTeamId(jwtToken);
    }
    
    /**
     * JWT 토큰에서 성별 추출
     */
    private String getGenderFromToken(String jwtToken) {
        return jwtProvider.getGender(jwtToken);
    }
    
    /**
     * JWT 토큰에서 나이 추출
     */
    private int getAgeFromToken(String jwtToken) {
        return jwtProvider.getAge(jwtToken);
    }

    /**
     * 에러 응답 전송
     */
    private void sendErrorResponse(String requestId, String errorMessage) {
        if (requestId != null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("requestId", requestId);
            errorResult.put("timestamp", LocalDateTime.now().toString());
            errorResult.put("errorMessage", errorMessage);

            chatAuthKafkaProducer.sendAuthResult(requestId, errorResult);
        }
    }

    /**
     * 채팅 요청 객체 생성 (하위 클래스에서 구현)
     */
    protected abstract ChatAuthRequest createChatAuthRequest(JsonNode requestNode, String requestId, String chatType);
}