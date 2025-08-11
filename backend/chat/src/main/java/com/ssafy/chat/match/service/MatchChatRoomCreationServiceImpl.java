package com.ssafy.chat.match.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.common.dto.AuthResult;
import com.ssafy.chat.common.dto.UserInfo;
import com.ssafy.chat.common.util.ChatRoomUtils;
import com.ssafy.chat.common.util.KSTTimeUtil;
import com.ssafy.chat.config.ChatConfiguration;
import com.ssafy.chat.global.constants.ChatRedisKey;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.MatchChatRoom;
import com.ssafy.chat.match.dto.MatchChatRoomCreateRequest;
import com.ssafy.chat.match.dto.MatchChatRoomCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 매칭 채팅방 생성 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatRoomCreationServiceImpl implements MatchChatRoomCreationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatAuthRequestProducer chatAuthRequestProducer;
    private final ChatAuthResultService chatAuthResultService;
    private final ChatConfiguration chatConfiguration;
    private final ChatRoomUtils chatRoomUtils;

    @Override
    public MatchChatRoomCreateResponse createMatchChatRoom(MatchChatRoomCreateRequest request, String jwtToken) {
        try {
            // 1. 사용자 인증 및 권한 검증
            AuthResult authResult = authenticateUser(jwtToken, request);

            // 2. 중복 채팅방 체크
            if (isDuplicateChatRoom(request.getGameId(), authResult.getUserInfo().getUserId())) {
                throw new ApiException(ErrorCode.DUPLICATE_MATCH_CHAT_ROOM);
            }

            // 3. 채팅방 생성
            MatchChatRoom chatRoom = buildMatchChatRoom(request, authResult);

            // 4. Redis에 저장
            saveChatRoomToRedis(chatRoom, getGameDateStr(authResult));

            // 5. 응답 변환
            return convertToResponse(chatRoom);

        } catch (ApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("매칭 채팅방 직렬화 실패 - gameId: {}", request.getGameId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        } catch (Exception e) {
            log.error("매칭 채팅방 생성 실패 - gameId: {}", request.getGameId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public boolean canCreateChatRoom(String jwtToken, Long gameId) {
        try {
            // 임시 요청 객체로 인증 확인
            MatchChatRoomCreateRequest tempRequest = MatchChatRoomCreateRequest.builder()
                    .gameId(gameId)
                    .matchTitle("권한 확인용")
                    .nickname("temp")
                    .build();

            AuthResult authResult = authenticateUser(jwtToken, tempRequest);
            return authResult.isSuccess();

        } catch (Exception e) {
            log.debug("채팅방 생성 권한 없음 - gameId: {}", gameId, e);
            return false;
        }
    }

    @Override
    public boolean isDuplicateChatRoom(Long gameId, Long creatorId) {
        try {
            // 같은 게임에 대해 이미 생성한 채팅방이 있는지 확인
            String pattern = "match_" + gameId + "_*";
            // Redis에서 패턴 검색을 통해 중복 체크
            // 실제 구현에서는 생성자 정보도 함께 확인해야 함

            log.debug("중복 채팅방 체크 - gameId: {}, creatorId: {}", gameId, creatorId);
            return false; // 임시로 false 반환

        } catch (Exception e) {
            log.error("중복 채팅방 체크 실패 - gameId: {}, creatorId: {}", gameId, creatorId, e);
            return false;
        }
    }

    /**
     * 사용자 인증
     */
    private AuthResult authenticateUser(String jwtToken, MatchChatRoomCreateRequest request) {
        // 채팅방 정보 생성
        Map<String, Object> roomInfo = createRoomInfo(request);

        // bbatty 서버에 인증 요청 전송
        String requestId = chatAuthRequestProducer.sendMatchChatCreateRequest(
                jwtToken, request.getGameId(), roomInfo, request.getNickname());

        if (requestId == null) {
            throw new ApiException(ErrorCode.KAFKA_MESSAGE_SEND_FAILED);
        }

        // 인증 결과 대기
        Map<String, Object> authResultMap = chatAuthResultService.waitForAuthResult(
                requestId, (int) chatRoomUtils.getAuthTimeoutMs());

        if (authResultMap == null) {
            log.error("bbatty 서버 인증 응답 타임아웃 - requestId: {}", requestId);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }

        // AuthResult 객체로 변환
        return mapToAuthResult(authResultMap);
    }

    /**
     * 채팅방 정보 생성
     */
    private Map<String, Object> createRoomInfo(MatchChatRoomCreateRequest request) {
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("gameId", request.getGameId());
        roomInfo.put("title", request.getTitle());
        roomInfo.put("nickname", request.getNickname());
        roomInfo.put("genderRestriction", request.getGenderRestriction());
        roomInfo.put("ageRestriction", request.getAgeRestriction());
        roomInfo.put("roomType", "MATCH");
        return roomInfo;
    }

    /**
     * MatchChatRoom 객체 생성
     */
    private MatchChatRoom buildMatchChatRoom(MatchChatRoomCreateRequest request, AuthResult authResult) {
        UserInfo userInfo = authResult.getUserInfo();
        String matchId = generateMatchId(request.getGameId());

        return MatchChatRoom.builder()
                .matchId(matchId)
                .gameId(request.getGameId())
                .matchTitle(request.getTitle())
                .matchDescription(request.getMatchDescription())
                .teamId(request.getTeamId())
                .minAge(request.getMinAge())
                .maxAge(request.getMaxAge())
                .genderCondition(request.getGenderCondition())
                .maxParticipants(request.getMaxParticipants())
                .minWinRate(request.getMinWinRate())
                .ownerId(String.valueOf(userInfo.getUserId()))
                .creatorNickname(request.getNickname())
                .currentParticipants(1) // 생성자 포함
                .createdAt(KSTTimeUtil.nowAsString())
                .lastActivityAt(KSTTimeUtil.nowAsString())
                .status("ACTIVE")
                .build();
    }

    /**
     * Redis에 채팅방 저장
     */
    private void saveChatRoomToRedis(MatchChatRoom chatRoom, String gameDate) throws JsonProcessingException {
        String matchRoomKey = ChatRedisKey.getMatchRoomInfoKey(chatRoom.getMatchId());
        String roomJson = objectMapper.writeValueAsString(chatRoom);

        // 채팅방 정보 저장
        redisTemplate.opsForValue().set(matchRoomKey, roomJson);

        // 전체 목록에 추가
        long timestamp = KSTTimeUtil.parseToTimestamp(chatRoom.getCreatedAt());
        redisTemplate.opsForZSet().add(ChatRedisKey.MATCH_ROOM_LIST, chatRoom.getMatchId(), timestamp);

        // 날짜별 목록에 추가
        String dateListKey = ChatRedisKey.getMatchRoomListByDateKey(gameDate);
        redisTemplate.opsForSet().add(dateListKey, chatRoom.getMatchId());

        log.info("매칭 채팅방 Redis 저장 완료 - matchId: {}", chatRoom.getMatchId());
    }

    /**
     * 응답 객체 생성
     */
    private MatchChatRoomCreateResponse convertToResponse(MatchChatRoom chatRoom) {
        return MatchChatRoomCreateResponse.builder()
                .matchId(chatRoom.getMatchId())
                .gameId(chatRoom.getGameId())
                .matchTitle(chatRoom.getMatchTitle())
                .matchDescription(chatRoom.getMatchDescription())
                .teamId(chatRoom.getTeamId())
                .minAge(chatRoom.getMinAge())
                .maxAge(chatRoom.getMaxAge())
                .genderCondition(chatRoom.getGenderCondition())
                .maxParticipants(chatRoom.getMaxParticipants())
                .currentParticipants(chatRoom.getCurrentParticipants())
                .minWinRate(chatRoom.getMinWinRate())
                .creatorNickname(chatRoom.getCreatorNickname())
                .createdAt(chatRoom.getCreatedAt())
                .status(chatRoom.getStatus())
                .build();
    }

    /**
     * 매치 ID 생성
     */
    private String generateMatchId(Long gameId) {
        return "match_" + gameId + "_" + Long.toHexString(System.currentTimeMillis());
    }


    /**
     * Map을 AuthResult로 변환
     */
    private AuthResult mapToAuthResult(Map<String, Object> authResultMap) {
        Boolean success = (Boolean) authResultMap.get("success");

        if (!Boolean.TRUE.equals(success)) {
            String errorMessage = (String) authResultMap.get("errorMessage");
            throw new ApiException(mapErrorMessage(errorMessage));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfoMap = (Map<String, Object>) authResultMap.get("userInfo");

        UserInfo userInfo = UserInfo.builder()
                .userId(userInfoMap.get("userId") != null ? ((Number) userInfoMap.get("userId")).longValue() : null)
                .nickname((String) userInfoMap.get("nickname"))
                .teamId(userInfoMap.get("teamId") != null ? ((Number) userInfoMap.get("teamId")).longValue() : null)
                .teamName((String) userInfoMap.get("teamName"))
                .age(userInfoMap.get("age") != null ? ((Number) userInfoMap.get("age")).intValue() : null)
                .gender((String) userInfoMap.get("gender"))
                .profileImgUrl((String) userInfoMap.get("profileImgUrl"))
                .winRate(userInfoMap.get("winRate") != null ? ((Number) userInfoMap.get("winRate")).doubleValue() : null)
                .isWinFairy(userInfoMap.get("isWinFairy") != null ? (Boolean) userInfoMap.get("isWinFairy") : false)
                .build();

        return AuthResult.builder()
                .success(success)
                .userInfo(userInfo)
                .requestId((String) authResultMap.get("requestId"))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 게임 날짜 문자열 추출
     */
    private String getGameDateStr(AuthResult authResult) {
        // AuthResult에서 게임 날짜 정보를 추출하거나 현재 날짜 사용
        return KSTTimeUtil.todayAsString();
    }

    /**
     * 에러 메시지를 ErrorCode로 매핑
     */
    private ErrorCode mapErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return ErrorCode.UNAUTHORIZED;
        }

        if (errorMessage.contains("경기 정보를 찾을 수 없어요")) {
            return ErrorCode.GAME_NOT_FOUND;
        } else if (errorMessage.contains("이미 종료된 경기예요")) {
            return ErrorCode.GAME_FINISHED;
        } else {
            return ErrorCode.UNAUTHORIZED;
        }
    }
}