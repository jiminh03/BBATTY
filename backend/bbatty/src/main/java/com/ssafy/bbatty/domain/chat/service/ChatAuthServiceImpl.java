package com.ssafy.bbatty.domain.chat.service;

import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.dto.response.ChatAuthResponse;
import com.ssafy.bbatty.domain.chat.kafka.ChatAuthKafkaProducer;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.GameStatus;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 채팅 인증/인가 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatAuthServiceImpl implements ChatAuthService {
    
    private final GameRepository gameRepository;
    private final ChatAuthKafkaProducer chatAuthKafkaProducer;
    private final JwtProvider jwtProvider;
    
    @Override
    public ApiResponse<ChatAuthResponse> authorizeChatAccess(Long userId, ChatAuthRequest request) {
        try {
            // 1. JWT 토큰에서 사용자 정보 파싱 (SecurityContext에서 이미 파싱된 정보 사용)
            String token = getCurrentJwtToken();
            Long userTeamId = jwtProvider.getTeamId(token);
            String userGender = jwtProvider.getGender(token);
            int userAge = jwtProvider.getAge(token);
            
            // 2. 채팅 유형별 권한 검증
            validateChatPermission(userId, userTeamId, request);
            
            // 3. 채팅방 정보 생성
            ChatAuthResponse.ChatRoomInfo chatRoomInfo = createChatRoomInfo(request);
            
            // 4. 사용자 정보 생성 (JWT에서 파싱한 정보 사용)
            ChatAuthResponse.UserInfo userInfo = createUserInfoFromJwt(userId, userTeamId, userGender, userAge);
            
            // 5. Kafka로 인증 성공 결과 전송
            sendAuthSuccessToKafka(request.getRequestId(), userInfo, chatRoomInfo);
            
            log.info("채팅 인증 성공: userId={}, chatType={}, action={}", 
                    userId, request.getChatType(), request.getAction());
            
            return ChatAuthResponse.success(request.getRequestId(), userInfo, chatRoomInfo);
            
        } catch (ApiException e) {
            log.warn("채팅 인증 실패: userId={}, error={}", userId, e.getMessage());
            
            // Kafka로 인증 실패 결과 전송
            sendAuthFailureToKafka(request.getRequestId(), e.getMessage());
            
            return ChatAuthResponse.failure(request.getRequestId(), e.getMessage());
        }
    }
    
    /**
     * 현재 JWT 토큰 가져오기
     */
    private String getCurrentJwtToken() {
        // SecurityContext에서 토큰 추출 로직 구현
        // 실제 구현에서는 HttpServletRequest에서 Authorization 헤더 파싱
        return ""; // TODO: 실제 토큰 추출 로직 구현
    }
    
    /**
     * 채팅 유형별 권한 검증
     */
    private void validateChatPermission(Long userId, Long userTeamId, ChatAuthRequest request) {
        String chatType = request.getChatType();
        
        if ("MATCH".equals(chatType)) {
            validateMatchChatPermission(request);
        } else if ("WATCH".equals(chatType)) {
            validateWatchChatPermission(userTeamId, request);
        } else {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
    
    /**
     * 매칭 채팅 권한 검증 (간단한 검증만, 상세 로직은 chat 서비스에서 처리)
     */
    private void validateMatchChatPermission(ChatAuthRequest request) {
        if (request.getMatchId() == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        // 경기 존재 여부만 확인
        Game game = gameRepository.findById(request.getMatchId())
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
        
        // 기본적인 경기 상태 확인
        if (game.getStatus() == GameStatus.FINISHED) {
            throw new ApiException(ErrorCode.GAME_FINISHED);
        }
    }
    
    /**
     * 직관 채팅 권한 검증 (사용자 팀 == 응원 팀)
     */
    private void validateWatchChatPermission(Long userTeamId, ChatAuthRequest request) {
        if (request.getMatchId() == null || request.getTeamId() == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        Game game = gameRepository.findById(request.getMatchId())
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
        
        // 경기 상태 확인 (라이브 경기만 직관 가능)
        if (game.getStatus() != GameStatus.LIVE) {
            throw new ApiException(ErrorCode.GAME_NOT_LIVE);
        }
        
        // 응원할 팀이 경기에 참여하는지 확인
        Long supportTeamId = request.getTeamId();
        if (!game.getHomeTeamId().equals(supportTeamId) && !game.getAwayTeamId().equals(supportTeamId)) {
            throw new ApiException(ErrorCode.TEAM_NOT_IN_GAME);
        }
        
        // 사용자 팀과 응원 팀이 같은지 확인
        if (!userTeamId.equals(supportTeamId)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_TEAM_ACCESS);
        }
    }
    
    /**
     * 채팅방 정보 생성
     */
    private ChatAuthResponse.ChatRoomInfo createChatRoomInfo(ChatAuthRequest request) {
        String roomId;
        boolean isNewRoom = false;
        
        if ("CREATE".equals(request.getAction())) {
            roomId = generateRoomId(request);
            isNewRoom = true;
        } else {
            roomId = request.getRoomId();
        }
        
        return ChatAuthResponse.ChatRoomInfo.builder()
                .roomId(roomId)
                .chatType(request.getChatType())
                .matchId(request.getMatchId())
                .roomName("채팅방")
                .isNewRoom(isNewRoom)
                .build();
    }
    
    /**
     * JWT에서 파싱한 사용자 정보로 UserInfo 생성
     */
    private ChatAuthResponse.UserInfo createUserInfoFromJwt(Long userId, Long teamId, String gender, int age) {
        return ChatAuthResponse.UserInfo.builder()
                .userId(userId)
                .nickname("") // JWT에 없는 정보는 빈 값으로
                .profileImg("")
                .teamId(teamId)
                .teamName("")
                .age(age)
                .gender(gender)
                .build();
    }
    
    /**
     * 채팅방 ID 생성
     */
    private String generateRoomId(ChatAuthRequest request) {
        if ("WATCH".equals(request.getChatType())) {
            // 직관 채팅: 경기ID + 팀ID 조합
            return "watch-" + request.getMatchId() + "-" + request.getTeamId();
        } else {
            // 매칭 채팅: 경기ID + 타임스탬프
            return "match-" + request.getMatchId() + "-" + System.currentTimeMillis();
        }
    }
    
    /**
     * Kafka로 인증 성공 결과 전송
     */
    private void sendAuthSuccessToKafka(String requestId, ChatAuthResponse.UserInfo userInfo, 
                                      ChatAuthResponse.ChatRoomInfo chatRoomInfo) {
        Map<String, Object> authResult = Map.of(
                "success", true,
                "requestId", requestId,
                "timestamp", LocalDateTime.now().toString(),
                "userInfo", userInfo,
                "chatRoomInfo", chatRoomInfo
        );
        
        chatAuthKafkaProducer.sendAuthResult(requestId, authResult);
    }
    
    /**
     * Kafka로 인증 실패 결과 전송
     */
    private void sendAuthFailureToKafka(String requestId, String errorMessage) {
        Map<String, Object> authResult = Map.of(
                "success", false,
                "requestId", requestId,
                "timestamp", LocalDateTime.now().toString(),
                "errorMessage", errorMessage
        );
        
        chatAuthKafkaProducer.sendAuthResult(requestId, authResult);
    }
}