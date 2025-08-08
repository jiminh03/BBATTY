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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
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
    public ApiResponse<ChatAuthResponse> authorizeChatAccess(Long userId, Long userTeamId, String userGender, 
                                                           int userAge, String userNickname, ChatAuthRequest request) {
        try {
            // 1. 채팅 유형별 권한 검증
            validateChatPermission(userId, userTeamId, userGender, userAge, request);
            
            // 2. 채팅방 정보 생성
            ChatAuthResponse.ChatRoomInfo chatRoomInfo = createChatRoomInfo(request);
            
            // 3. 사용자 정보 생성 (전달받은 정보 사용)
            ChatAuthResponse.UserInfo userInfo = createUserInfo(userId, userTeamId, userGender, userAge, userNickname);
            
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
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7); // "Bearer " 제거
            }
            
            log.warn("Authorization 헤더가 없거나 Bearer 토큰이 아닙니다");
            throw new ApiException(ErrorCode.UNAUTHORIZED);
            
        } catch (IllegalStateException e) {
            // Kafka 요청인 경우 HTTP 컨텍스트가 없을 수 있음
            log.warn("HTTP 요청 컨텍스트를 가져올 수 없습니다 - Kafka 요청일 가능성");
            return null; // Kafka Consumer에서 직접 토큰을 처리하므로 null 반환
        }
    }
    
    /**
     * 채팅 유형별 권한 검증
     */
    private void validateChatPermission(Long userId, Long userTeamId, String userGender, int userAge, ChatAuthRequest request) {
        String chatType = request.getChatType();
        
        if ("MATCH".equals(chatType)) {
            validateMatchChatPermission(request);
            // 매칭방 조건 검증 (CREATE/JOIN 모두)
            validateMatchRoomConditions(userId, userTeamId, userGender, userAge, request);
        } else if ("WATCH".equals(chatType)) {
            validateWatchChatPermission(userTeamId, request);
        } else {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 매칭 채팅 권한 검증
     */
    private void validateMatchChatPermission(ChatAuthRequest request) {
        if (request.getGameId() == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 경기 존재 여부 확인
        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));

        // 기본적인 경기 상태 확인
        if (game.getStatus() == GameStatus.FINISHED) {
            throw new ApiException(ErrorCode.GAME_FINISHED);
        }
    }
    
    /**
     * 매칭방 조건 검증 (CREATE/JOIN 공통)
     */
    private void validateMatchRoomConditions(Long userId, Long userTeamId, String userGender, 
                                           int userAge, ChatAuthRequest request) {
        if (request.getRoomInfo() == null) {
            log.debug("roomInfo가 없어 매칭방 조건 검증을 생략합니다. userId={}", userId);
            return; // roomInfo가 없으면 검증 생략
        }
        
        Map<String, Object> roomInfo = request.getRoomInfo();
        
        try {
            // 1. 나이 조건 검증
            validateAgeCondition(userAge, roomInfo);
            
            // 2. 성별 조건 검증  
            validateGenderCondition(userGender, roomInfo);
            
            // 3. 팀 조건 검증 (allowOtherTeams 고려)
            validateTeamCondition(userTeamId, roomInfo);
            
            // 4. 승률 조건 검증
            validateWinRateCondition(userId, roomInfo);
            
            log.info("매칭방 조건 검증 완료: userId={}, age={}, gender={}, teamId={}", 
                    userId, userAge, userGender, userTeamId);
                    
        } catch (ClassCastException | NumberFormatException e) {
            log.error("매칭방 조건 정보 파싱 오류: userId={}, error={}", userId, e.getMessage());
            throw new ApiException(ErrorCode.MATCH_ROOM_CONDITIONS_INVALID);
        }
    }
    
    /**
     * 나이 조건 검증
     */
    private void validateAgeCondition(int userAge, Map<String, Object> roomInfo) {
        Object minAgeObj = roomInfo.get("minAge");
        Object maxAgeObj = roomInfo.get("maxAge");
        
        if (minAgeObj != null && maxAgeObj != null) {
            int minAge = ((Number) minAgeObj).intValue();
            int maxAge = ((Number) maxAgeObj).intValue();
            
            if (userAge < minAge || userAge > maxAge) {
                throw new ApiException(ErrorCode.AGE_CONDITION_NOT_MET);
            }
        }
    }
    
    /**
     * 성별 조건 검증
     */
    private void validateGenderCondition(String userGender, Map<String, Object> roomInfo) {
        Object genderConditionObj = roomInfo.get("genderCondition");
        
        if (genderConditionObj != null) {
            String genderCondition = (String) genderConditionObj;
            
            if (!"ALL".equals(genderCondition) && !genderCondition.equals(userGender)) {
                throw new ApiException(ErrorCode.GENDER_CONDITION_NOT_MET);
            }
        }
    }
    
    /**
     * 팀 조건 검증 (allowOtherTeams 필드 고려)
     */
    private void validateTeamCondition(Long userTeamId, Map<String, Object> roomInfo) {
        Object allowOtherTeamsObj = roomInfo.get("allowOtherTeams");
        Object roomTeamIdObj = roomInfo.get("teamId");
        
        if (allowOtherTeamsObj != null && roomTeamIdObj != null) {
            boolean allowOtherTeams = (Boolean) allowOtherTeamsObj;
            Long roomTeamId = ((Number) roomTeamIdObj).longValue();
            
            // allowOtherTeams가 false인 경우, 같은 팀만 참여 가능
            if (!allowOtherTeams && !userTeamId.equals(roomTeamId)) {
                throw new ApiException(ErrorCode.UNAUTHORIZED_TEAM_ACCESS);
            }
        }
    }
    
    /**
     * 승률 조건 검증
     */
    private void validateWinRateCondition(Long userId, Map<String, Object> roomInfo) {
        Object minWinRateObj = roomInfo.get("minWinRate");
        
        if (minWinRateObj != null) {
            int minWinRate = ((Number) minWinRateObj).intValue();
            
            // 사용자의 실제 승률을 계산 (실제 구현에서는 UserService나 별도 서비스에서 계산)
            double userWinRate = calculateUserWinRate(userId);
            
            if (userWinRate < minWinRate) {
                throw new ApiException(ErrorCode.WIN_RATE_CONDITION_NOT_MET);
            }
        }
    }
    
    /**
     * 사용자 승률 계산 (임시 구현 - 실제로는 별도 서비스에서 처리)
     */
    private double calculateUserWinRate(Long userId) {
        // TODO: 실제 승률 계산 로직 구현
        // 현재는 임시로 70.0 반환
        return 70.0;
    }
    
    /**
     * 직관 채팅 권한 검증 (사용자 팀 == 응원 팀)
     */
    private void validateWatchChatPermission(Long userTeamId, ChatAuthRequest request) {
        if (request.getGameId() == null || request.getRoomInfo() == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {
            // roomInfo에서 응원할 팀 정보 추출
            Map<String, Object> roomInfo = request.getRoomInfo();
            Object teamIdObj = roomInfo.get("teamId");
            
            if (teamIdObj == null) {
                log.error("직관 채팅방 정보에 teamId가 없습니다. roomInfo={}", roomInfo);
                throw new ApiException(ErrorCode.ROOM_INFO_MISSING);
            }
            
            Long supportTeamId = ((Number) teamIdObj).longValue();

            // 경기 정보 확인
            Game game = gameRepository.findById(request.getGameId())
                    .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));

            // 경기 상태 확인 (종료된 경기는 직관 채팅 불가)
            if (game.getStatus() == GameStatus.FINISHED) {
                throw new ApiException(ErrorCode.GAME_FINISHED);
            }

            // 응원할 팀이 경기에 참여하는지 확인
            if (!game.getHomeTeamId().equals(supportTeamId) && !game.getAwayTeamId().equals(supportTeamId)) {
                log.warn("경기에 참여하지 않는 팀으로 직관 채팅 시도: gameId={}, supportTeamId={}, homeTeamId={}, awayTeamId={}", 
                        request.getGameId(), supportTeamId, game.getHomeTeamId(), game.getAwayTeamId());
                throw new ApiException(ErrorCode.TEAM_NOT_IN_GAME);
            }

            // 사용자 팀과 응원 팀이 같은지 확인
            if (!userTeamId.equals(supportTeamId)) {
                log.info("다른 팀 직관 채팅 시도: userId의 teamId={}, 응원 teamId={}", userTeamId, supportTeamId);
                throw new ApiException(ErrorCode.UNAUTHORIZED_TEAM_ACCESS);
            }

            log.info("직관 채팅 권한 검증 완료: userId teamId={}, supportTeamId={}, gameId={}", 
                    userTeamId, supportTeamId, request.getGameId());
                    
        } catch (ClassCastException | NumberFormatException e) {
            log.error("직관 채팅방 정보 파싱 오류: roomInfo={}, error={}", request.getRoomInfo(), e.getMessage());
            throw new ApiException(ErrorCode.ROOM_INFO_MISSING);
        }
    }

    /**
     * 채팅방 정보 생성
     */
    private ChatAuthResponse.ChatRoomInfo createChatRoomInfo(ChatAuthRequest request) {
        // roomId는 chat 서버에서 생성하므로 여기서는 기본 정보만 설정
        return ChatAuthResponse.ChatRoomInfo.builder()
                .roomId(request.getRoomId())         // roomId 사용 (최상위 개념)
                .chatType(request.getChatType())
                .gameId(request.getGameId())         // matchId → gameId로 변경
                .roomName("채팅방")
                .isNewRoom("CREATE".equals(request.getAction()))
                .build();
    }

    /**
     * 전달받은 정보로 UserInfo 생성
     */
    private ChatAuthResponse.UserInfo createUserInfo(Long userId, Long teamId, String gender, int age, String nickname) {
        return ChatAuthResponse.UserInfo.builder()
                .userId(userId)
                .nickname(nickname)
                .profileImg("") // 프로필 이미지는 별도로 관리
                .teamId(teamId)
                .teamName("") // 팀명은 별도 조회 필요
                .age(age)
                .gender(gender)
                .build();
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