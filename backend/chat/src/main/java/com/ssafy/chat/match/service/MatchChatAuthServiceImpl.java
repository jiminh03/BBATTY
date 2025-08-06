package com.ssafy.chat.match.service;

import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.MatchChatJoinRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 매칭 채팅 인증 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatAuthServiceImpl implements MatchChatAuthService {

    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;
    private final MatchChatRoomService matchChatRoomService;

    private static final String SESSION_KEY_PREFIX = "match_chat_session:";
    private static final Duration SESSION_EXPIRE_TIME = Duration.ofHours(2);

    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, MatchChatJoinRequest request) {
        try {
            // 1. matchId 유효성 검증
            validateMatchId(request.getMatchId());
            
            // 2. TODO: 나중에 Kafka로 bbatty 서버에 인증 요청
            // 현재는 모든 요청 허용하고 더미 데이터로 세션 생성
            
            // 닉네임과 matchId 기반으로 고유한 더미 데이터 생성
            Long userId = (long) Math.abs((request.getNickname() + request.getMatchId()).hashCode() % 10000 + 1);
            String gender = userId % 2 == 0 ? "M" : "F"; // 짝수면 남성, 홀수면 여성
            Integer age = (int) (userId % 30 + 20); // 20-49세
            Long teamId = (long) (userId % 2 + 1); // 1팀 또는 2팀

            log.info("매칭 채팅 요청 허용 - 더미 데이터 사용 - userId: {}, gender: {}, age: {}, teamId: {}", 
                    userId, gender, age, teamId);

            Map<String, Object> sessionInfo = createSessionInfo(userId, request, gender, age, teamId);

            String sessionToken = generateSessionToken();
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            
            redisUtil.setValue(sessionKey, sessionInfo, SESSION_EXPIRE_TIME);
            
            log.info("매칭 채팅 세션 생성 완료 - userId: {}, matchId: {}, sessionToken: {}", 
                    userId, request.getMatchId(), sessionToken);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionToken", sessionToken);
            response.put("userId", userId.toString());
            response.put("matchId", request.getMatchId());
            response.put("expiresIn", SESSION_EXPIRE_TIME.getSeconds());

            return response;

        } catch (ApiException e) {
            throw e; // ApiException은 그대로 다시 던지기
        } catch (Exception e) {
            log.error("매칭 채팅 세션 생성 실패", e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "세션 생성에 실패했습니다.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfoByToken(String sessionToken) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            Object sessionData = redisUtil.getValue(sessionKey);
            
            if (sessionData == null) {
                throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN, "유효하지 않은 세션 토큰입니다.");
            }

            Map<String, Object> userInfo = (Map<String, Object>) sessionData;
            
            log.debug("세션 토큰 검증 성공 - userId: {}, matchId: {}", 
                    userInfo.get("userId"), userInfo.get("matchId"));
            
            return userInfo;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("세션 토큰 검증 실패 - token: {}", sessionToken, e);
            throw new ApiException(ErrorCode.INVALID_SESSION_TOKEN, "세션 검증에 실패했습니다.");
        }
    }

    @Override
    public void invalidateSession(String sessionToken) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            redisUtil.deleteKey(sessionKey);
            log.info("세션 무효화 완료 - sessionToken: {}", sessionToken);
        } catch (Exception e) {
            log.error("세션 무효화 실패 - sessionToken: {}", sessionToken, e);
        }
    }

    // 더 이상 사용하지 않음 - 모든 요청 허용
    // private void validateMatchRoomAccess(...) { ... }

    private Map<String, Object> createSessionInfo(Long userId, MatchChatJoinRequest request, 
                                                String gender, Integer age, Long teamId) {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        sessionInfo.put("userId", userId.toString());
        sessionInfo.put("gender", gender);
        sessionInfo.put("age", age);
        sessionInfo.put("teamId", teamId);
        
        sessionInfo.put("matchId", request.getMatchId());
        sessionInfo.put("userName", request.getNickname());
        sessionInfo.put("nickname", request.getNickname());
        sessionInfo.put("winRate", request.getWinRate());
        sessionInfo.put("profileImgUrl", request.getProfileImgUrl());
        sessionInfo.put("isWinFairy", request.isWinFairy());
        
        sessionInfo.put("createdAt", System.currentTimeMillis());
        
        return sessionInfo;
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * matchId 유효성 검증
     */
    private void validateMatchId(String matchId) {
        if (matchId == null || matchId.trim().isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "matchId가 비어있습니다.");
        }
        
        // Redis에서 매칭 채팅방 존재 여부 확인
        try {
            var matchRoom = matchChatRoomService.getMatchChatRoom(matchId);
            if (matchRoom == null) {
                throw new ApiException(ErrorCode.MATCH_CHAT_ROOM_NOT_FOUND, "존재하지 않는 매칭 채팅방입니다.");
            }
            
            // 채팅방 상태 검증
            if (!"ACTIVE".equals(matchRoom.getStatus())) {
                throw new ApiException(ErrorCode.MATCH_CHAT_ROOM_CLOSED, "비활성 상태의 매칭 채팅방입니다.");
            }
            
            log.debug("매칭 채팅방 유효성 검증 성공 - matchId: {}, gameId: {}", matchId, matchRoom.getGameId());
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("매칭 채팅방 유효성 검증 중 오류 - matchId: {}", matchId, e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "매칭 채팅방 검증에 실패했습니다.");
        }
    }
}
