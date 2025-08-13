package com.ssafy.chat.match.service;

import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.common.dto.AuthResult;
import com.ssafy.chat.common.dto.SessionToken;
import com.ssafy.chat.common.dto.SessionTokenInfo;
import com.ssafy.chat.common.dto.UserInfo;
import com.ssafy.chat.common.service.SessionTokenService;
import com.ssafy.chat.common.util.ChatRoomUtils;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.MatchChatJoinRequest;
import com.ssafy.chat.match.dto.MatchChatRoomCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 매칭 채팅방 인증 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatRoomAuthServiceImpl implements MatchChatRoomAuthService {
    
    private final ChatAuthRequestProducer chatAuthRequestProducer;
    private final ChatAuthResultService chatAuthResultService;
    private final ChatRoomUtils chatRoomUtils;
    private final SessionTokenService sessionTokenService;
    
    @Override
    public AuthResult authenticateForCreation(String jwtToken, MatchChatRoomCreateRequest request) {
        try {
            // 채팅방 정보 생성
            Map<String, Object> roomInfo = createRoomInfo(request);
            
            // bbatty 서버에 인증 요청
            String requestId = chatAuthRequestProducer.sendMatchChatCreateRequest(
                    jwtToken, request.getGameId(), roomInfo, request.getMatchTitle());
            
            if (requestId == null) {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            // 인증 결과 대기
            Map<String, Object> authResultMap = chatAuthResultService.waitForAuthResult(
                    requestId, (int) chatRoomUtils.getAuthTimeoutMs());
            
            if (authResultMap == null) {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            // AuthResult 객체로 변환
            return mapToAuthResult(authResultMap);
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("매칭 채팅방 인증 실패 - gameId: {}", request.getGameId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }
    
    /**
     * 채팅방 정보 Map 생성
     */
    private Map<String, Object> createRoomInfo(MatchChatRoomCreateRequest request) {
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("gameId", request.getGameId());
        roomInfo.put("minAge", request.getMinAge());
        roomInfo.put("maxAge", request.getMaxAge());
        roomInfo.put("genderCondition", request.getGenderCondition());
        roomInfo.put("maxParticipants", request.getMaxParticipants());
        roomInfo.put("minWinRate", request.getMinWinRate());
        return roomInfo;
    }
    
    /**
     * Map을 AuthResult로 변환
     */
    private AuthResult mapToAuthResult(Map<String, Object> authResultMap) {
        Boolean success = (Boolean) authResultMap.get("success");
        
        if (!Boolean.TRUE.equals(success)) {
            String errorMessage = (String) authResultMap.getOrDefault("errorMessage", "인증에 실패했습니다.");
            ErrorCode errorCode = mapErrorMessage(errorMessage);
            throw new ApiException(errorCode);
        }
        
        // 사용자 정보 파싱
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfoMap = (Map<String, Object>) authResultMap.get("userInfo");
        
        // 디버깅: 메인 서버 응답에서 profileImgUrl 확인
        log.info("메인 서버 userInfoMap: {}", userInfoMap);
        log.info("profileImgUrl 값: {}", userInfoMap.get("profileImgUrl"));
        
        UserInfo userInfo = UserInfo.builder()
                .userId(((Number) userInfoMap.get("userId")).longValue())
                .nickname((String) userInfoMap.get("nickname"))
                .teamId(userInfoMap.get("teamId") != null ? ((Number) userInfoMap.get("teamId")).longValue() : null)
                .teamName((String) userInfoMap.get("teamName"))
                .age(userInfoMap.get("age") != null ? ((Number) userInfoMap.get("age")).intValue() : null)
                .gender((String) userInfoMap.get("gender"))
                .profileImgUrl((String) userInfoMap.get("profileImgUrl"))
                .winRate(userInfoMap.get("winRate") != null ? ((Number) userInfoMap.get("winRate")).doubleValue() : null)
                .isWinFairy(userInfoMap.get("isWinFairy") != null ? (Boolean) userInfoMap.get("isWinFairy") : false)
                .build();
        
        // 디버깅: 생성된 UserInfo의 profileImgUrl 확인
        log.info("생성된 UserInfo - profileImgUrl: {}", userInfo.getProfileImgUrl());
        
        return AuthResult.builder()
                .success(true)
                .userInfo(userInfo)
                .additionalInfo(authResultMap)
                .build();
    }
    
    /**
     * 에러 메시지를 ErrorCode로 매핑
     */
    private ErrorCode mapErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return ErrorCode.UNAUTHORIZED;
        }
        
        // 게임 관련 오류
        if (errorMessage.contains("경기 정보를 찾을 수 없어요")) {
            return ErrorCode.GAME_NOT_FOUND;
        } else if (errorMessage.contains("이미 종료된 경기예요")) {
            return ErrorCode.GAME_FINISHED;
        } else if (errorMessage.contains("경기가 진행 중이 아닙니다")) {
            return ErrorCode.GAME_NOT_LIVE;
        }
        
        // 팀 관련 오류
        else if (errorMessage.contains("해당 팀이 경기에 참여하지 않습니다")) {
            return ErrorCode.TEAM_NOT_IN_GAME;
        } else if (errorMessage.contains("팀에 대한 접근 권한이 없습니다")) {
            return ErrorCode.UNAUTHORIZED_TEAM_ACCESS;
        }
        
        // 매칭 조건 관련 오류
        else if (errorMessage.contains("나이 조건에 맞지 않습니다")) {
            return ErrorCode.INVALID_MATCH_CONDITIONS;
        } else if (errorMessage.contains("성별 조건에 맞지 않습니다")) {
            return ErrorCode.INVALID_MATCH_CONDITIONS;
        } else if (errorMessage.contains("승률 조건에 맞지 않습니다")) {
            return ErrorCode.INVALID_MATCH_CONDITIONS;
        } else if (errorMessage.contains("매칭방 조건 정보가 올바르지 않습니다")) {
            return ErrorCode.INVALID_MATCH_CONDITIONS;
        } else if (errorMessage.contains("채팅방 정보가 누락되었습니다")) {
            return ErrorCode.REQUIRED_FIELD_MISSING;
        }
        
        // 인증 관련 오류
        else if (errorMessage.contains("유효하지 않은 토큰이에요")) {
            return ErrorCode.INVALID_SESSION_TOKEN;
        } else if (errorMessage.contains("인증이 필요해요")) {
            return ErrorCode.UNAUTHORIZED;
        } else if (errorMessage.contains("접근 권한이 없어요")) {
            return ErrorCode.FORBIDDEN;
        }
        
        // 입력값 관련 오류
        else if (errorMessage.contains("입력값이 올바르지 않습니다")) {
            return ErrorCode.INVALID_INPUT_VALUE;
        } else if (errorMessage.contains("잘못된 요청이에요")) {
            return ErrorCode.BAD_REQUEST;
        }
        
        // 기본값
        return ErrorCode.UNAUTHORIZED;
    }
    
    @Override
    public Map<String, Object> getUserInfoByToken(String sessionToken) {
        try {
            SessionTokenInfo tokenInfo = sessionTokenService.validateToken(sessionToken);
            if (tokenInfo == null || !tokenInfo.isValid()) {
                log.warn("유효하지 않은 세션 토큰: {}", sessionToken);
                return null;
            }
            
            // SessionTokenInfo를 Map으로 변환 (WebSocket 핸드셰이크에서 필요한 모든 정보 포함)
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", tokenInfo.getUserId());
            userInfo.put("nickname", tokenInfo.getNickname());
            userInfo.put("teamId", tokenInfo.getTeamId());
            userInfo.put("teamName", tokenInfo.getTeamName());
            userInfo.put("roomId", tokenInfo.getRoomId());
            userInfo.put("roomType", tokenInfo.getRoomType());
            userInfo.put("gameId", tokenInfo.getGameId());
            
            // WebSocket 핸드셰이크에서 필요한 추가 정보들
            userInfo.put("age", tokenInfo.getAge());
            userInfo.put("gender", tokenInfo.getGender());
            userInfo.put("winRate", tokenInfo.getWinRate());
            userInfo.put("isWinFairy", tokenInfo.getIsWinFairy());
            userInfo.put("profileImgUrl", tokenInfo.getProfileImgUrl()); // SessionTokenInfo에서 실제 값 가져오기
            
            log.debug("세션 토큰 검증 성공 - userId: {}, nickname: {}, teamId: {}", 
                    tokenInfo.getUserId(), tokenInfo.getNickname(), tokenInfo.getTeamId());
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("세션 토큰 검증 실패 - sessionToken: {}", sessionToken, e);
            return null;
        }
    }
    
    @Override
    public Map<String, Object> validateAndCreateSession(String jwtToken, MatchChatJoinRequest request) {
        try {
            // 1. 채팅방 정보 생성 (입장 요청용)
            Map<String, Object> roomInfo = createJoinRoomInfo(request);
            
            // 2. bbatty 서버에 입장 인증 요청
            String requestId = chatAuthRequestProducer.sendMatchChatJoinRequest(
                    jwtToken, request.getMatchId(), roomInfo, request.getNickname());
            
            if (requestId == null) {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            // 3. 인증 결과 대기
            Map<String, Object> authResultMap = chatAuthResultService.waitForAuthResult(
                    requestId, (int) chatRoomUtils.getAuthTimeoutMs());
            
            if (authResultMap == null) {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            // 4. AuthResult 객체로 변환
            AuthResult authResult = mapToAuthResult(authResultMap);
            
            // 5. 클라이언트 nickname으로 UserInfo 업데이트
            UserInfo updatedUserInfo = UserInfo.builder()
                    .userId(authResult.getUserInfo().getUserId())
                    .nickname(request.getNickname()) // 클라이언트에서 보낸 nickname 사용
                    .teamId(authResult.getUserInfo().getTeamId())
                    .teamName(authResult.getUserInfo().getTeamName())
                    .age(authResult.getUserInfo().getAge())
                    .gender(authResult.getUserInfo().getGender())
                    .profileImgUrl(authResult.getUserInfo().getProfileImgUrl())
                    .winRate(authResult.getUserInfo().getWinRate())
                    .isWinFairy(authResult.getUserInfo().getIsWinFairy())
                    .build();
            
            // 6. 세션 토큰 생성
            Long gameId = Long.parseLong(request.getMatchId().split("_")[1]);
            SessionToken sessionToken = sessionTokenService.createToken(
                    updatedUserInfo,
                    request.getMatchId(),
                    "MATCH",
                    gameId
            );
            
            // 7. 세션 정보 반환
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionToken", sessionToken.getToken());
            sessionData.put("userId", updatedUserInfo.getUserId());
            sessionData.put("nickname", updatedUserInfo.getNickname()); // 클라이언트 nickname 사용
            sessionData.put("teamId", updatedUserInfo.getTeamId());
            sessionData.put("teamName", updatedUserInfo.getTeamName());
            sessionData.put("expiresIn", sessionToken.getExpiresIn() * 1000); // 초를 밀리초로 변환
            
            return sessionData;
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("세션 생성 실패 - matchId: {}", request.getMatchId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }
    
    /**
     * 입장 요청용 채팅방 정보 생성
     */
    private Map<String, Object> createJoinRoomInfo(MatchChatJoinRequest request) {
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("matchId", request.getMatchId());
        roomInfo.put("nickname", request.getNickname());
        roomInfo.put("winRate", request.getWinRate());
        roomInfo.put("profileImgUrl", request.getProfileImgUrl());
        roomInfo.put("isWinFairy", request.isWinFairy());
        return roomInfo;
    }
}