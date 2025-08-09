package com.ssafy.bbatty.domain.chat.strategy;

import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.GameStatus;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 매칭 채팅 검증 전략
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatValidationStrategy implements ChatValidationStrategy {
    
    private final GameRepository gameRepository;
    
    @Override
    public void validateChatPermission(Long userId, Long userTeamId, String userGender, int userAge, ChatAuthRequest request) {
        // 1. 기본 매칭 채팅 권한 검증
        validateBasicMatchChatPermission(request);
        
        // 2. 매칭방 조건 검증 (CREATE/JOIN 공통)
        validateMatchRoomConditions(userId, userTeamId, userGender, userAge, request);
    }
    
    @Override
    public String getSupportedChatType() {
        return "MATCH";
    }
    
    /**
     * 기본 매칭 채팅 권한 검증
     */
    private void validateBasicMatchChatPermission(ChatAuthRequest request) {
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
            
            // 4. 승률 조건 검증 (클라이언트 승률 사용)
            validateWinRateCondition(roomInfo, request);
            
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
     * 승률 조건 검증 (클라이언트에서 전송한 승률 사용)
     */
    private void validateWinRateCondition(Map<String, Object> roomInfo, ChatAuthRequest request) {
        Object minWinRateObj = roomInfo.get("minWinRate");
        
        if (minWinRateObj != null) {
            int minWinRate = ((Number) minWinRateObj).intValue();
            
            // 클라이언트에서 전송한 사용자 승률 사용
            Integer userWinRate = getUserWinRateFromRequest(request);
            
            if (userWinRate == null) {
                log.warn("사용자 승률 정보가 없습니다. 승률 조건 검증을 생략합니다.");
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
            }
            
            if (userWinRate < minWinRate) {
                log.info("승률 조건 불충족: 사용자 승률={}%, 최소 승률={}%", userWinRate, minWinRate);
                throw new ApiException(ErrorCode.WIN_RATE_CONDITION_NOT_MET);
            }
            
            log.debug("승률 조건 충족: 사용자 승률={}%, 최소 승률={}%", userWinRate, minWinRate);
        }
    }
    
    /**
     * 요청에서 사용자 승률 추출
     */
    private Integer getUserWinRateFromRequest(ChatAuthRequest request) {
        if (request.getRoomInfo() == null) {
            return null;
        }
        
        Object winRateObj = request.getRoomInfo().get("winRate");
        if (winRateObj != null) {
            return ((Number) winRateObj).intValue();
        }
        
        return null;
    }
}