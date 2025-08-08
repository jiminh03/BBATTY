package com.ssafy.chat.match.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.match.dto.*;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.constants.ChatRedisKey;
import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.config.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 매칭 채팅방 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatRoomServiceImpl implements MatchChatRoomService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatAuthRequestProducer chatAuthRequestProducer;
    private final ChatAuthResultService chatAuthResultService;
    
    private static final String MATCH_ROOM_PREFIX = "match_room:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public MatchChatRoomCreateResponse createMatchChatRoom(MatchChatRoomCreateRequest request, String jwtToken) {
        try {
            
            // 1. bbatty 서버에 방 생성 인증 요청
            Map<String, Object> roomCreateInfo = new HashMap<>();
            roomCreateInfo.put("matchTitle", request.getMatchTitle());
            roomCreateInfo.put("matchDescription", request.getMatchDescription());
            roomCreateInfo.put("teamId", request.getTeamId());
            roomCreateInfo.put("minAge", request.getMinAge());
            roomCreateInfo.put("maxAge", request.getMaxAge());
            roomCreateInfo.put("genderCondition", request.getGenderCondition());
            roomCreateInfo.put("maxParticipants", request.getMaxParticipants());
            
            String requestId = chatAuthRequestProducer.sendMatchChatCreateRequest(
                jwtToken, request.getGameId(), roomCreateInfo, request.getNickname());
            
            if (requestId == null) {
                throw new ApiException(ErrorCode.SERVER_ERROR);
            }
            
            // 2. bbatty 서버 응답 대기
            Map<String, Object> authResult = chatAuthResultService.waitForAuthResult(requestId, 10000);
            
            if (authResult == null) {
                log.error("bbatty 서버 응답이 null - gameId: {}, requestId: {}", request.getGameId(), requestId);
                throw new ApiException(ErrorCode.SERVER_ERROR);
            }
            
            log.debug("bbatty 서버 응답 - gameId: {}, authResult: {}", request.getGameId(), authResult);
            
            Boolean success = (Boolean) authResult.get("success");
            if (success == null || !success) {
                String errorMessage = (String) authResult.get("errorMessage");
                log.error("bbatty 서버 인증 실패 - gameId: {}, errorMessage: {}", request.getGameId(), errorMessage);
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            // 3. bbatty 서버에서 인증된 사용자 정보 및 게임 정보 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) authResult.get("userInfo");
            @SuppressWarnings("unchecked")
            Map<String, Object> gameInfo = (Map<String, Object>) authResult.get("gameInfo");
            
            // 필수 데이터 검증
            if (userInfo == null) {
                log.error("bbatty 서버에서 userInfo가 null - gameId: {}", request.getGameId());
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            if (gameInfo == null) {
                log.error("bbatty 서버에서 gameInfo가 null - gameId: {}. 해당 경기가 존재하지 않거나 bbatty 응답에 문제가 있음", request.getGameId());
                throw new ApiException(ErrorCode.GAME_NOT_FOUND);
            }
            
            Long userId = ((Number) userInfo.get("userId")).longValue();
            String gameDateStr = (String) gameInfo.get("gameDate"); // bbatty에서 "yyyy-MM-dd" 형식으로 제공
            
            if (gameDateStr == null || gameDateStr.trim().isEmpty()) {
                log.error("bbatty 서버에서 gameDate가 null 또는 empty - gameId: {}, gameInfo: {}", request.getGameId(), gameInfo);
                throw new ApiException(ErrorCode.GAME_NOT_FOUND);
            }
            
            // 4. 경기 ID 기반으로 매칭 채팅방 ID 자동 생성
            String matchId = generateMatchId(request.getGameId());
            
            // 5. 매칭방 생성
            MatchChatRoom matchRoom = MatchChatRoom.builder()
                    .matchId(matchId)
                    .gameId(request.getGameId())
                    .matchTitle(request.getMatchTitle())
                    .matchDescription(request.getMatchDescription())
                    .teamId(request.getTeamId())
                    .minAge(request.getMinAge())
                    .maxAge(request.getMaxAge())
                    .genderCondition(request.getGenderCondition())
                    .maxParticipants(request.getMaxParticipants())
                    .currentParticipants(0)
                    .createdAt(LocalDateTime.now().toString())
                    .lastActivityAt(LocalDateTime.now().toString())
                    .status("ACTIVE")
                    .ownerId(userId.toString()) // bbatty 서버에서 인증된 실제 사용자 ID
                    .build();
            
            // 6. TTL 계산 (경기 날짜 자정까지)
            Duration ttl = calculateTTLFromGameDate(gameDateStr);
            
            // 7. Redis에 저장 - ChatRedisKey 사용
            String roomKey = ChatRedisKey.getMatchRoomInfoKey(matchId);
            String roomJson = objectMapper.writeValueAsString(matchRoom);
            redisTemplate.opsForValue().set(roomKey, roomJson, ttl);
            
            // 8. 전체 매칭방 목록에 추가
            long score = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(ChatRedisKey.MATCH_ROOM_LIST, matchId, score);
            redisTemplate.expire(ChatRedisKey.MATCH_ROOM_LIST, ttl);
            
            // 9. 날짜별 매칭방 목록에도 추가
            String dateListKey = ChatRedisKey.getMatchRoomListByDateKey(gameDateStr);
            redisTemplate.opsForSet().add(dateListKey, matchId);
            redisTemplate.expire(dateListKey, ttl);
            
            log.info("매칭 채팅방 생성 완료 - gameId: {}, matchId: {}", request.getGameId(), matchId);
            
            return convertToResponse(matchRoom);
            
        } catch (ApiException e) {
            throw e; // ApiException은 그대로 다시 던지기
        } catch (JsonProcessingException e) {
            log.error("매칭 채팅방 직렬화 실패 - gameId: {}", request.getGameId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        } catch (Exception e) {
            log.error("매칭 채팅방 생성 실패 - gameId: {}", request.getGameId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }
    
    @Override
    public MatchChatRoomListResponse getMatchChatRoomList(MatchChatRoomListRequest request) {
        try {
            // 키워드가 있는 경우와 없는 경우를 다르게 처리
            if (hasKeyword(request.getKeyword())) {
                return searchMatchChatRoomsWithKeyword(request);
            } else {
                return getMatchChatRoomsWithoutKeyword(request);
            }
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("매칭 채팅방 목록 조회 실패", e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }
    
    /**
     * 키워드 없이 매칭 채팅방 목록 조회 (최적화된 버전)
     */
    private MatchChatRoomListResponse getMatchChatRoomsWithoutKeyword(MatchChatRoomListRequest request) {
        // 클라이언트가 제공한 cursor 이전 데이터 조회
        long maxScore = Long.MAX_VALUE;
        if (request.getLastCreatedAt() != null) {
            maxScore = parseTimestampToScore(request.getLastCreatedAt()) - 1;
        }
        
        // 정확히 필요한 수만큼 조회 (키워드 필터링이 없으므로)
        Set<String> matchIds = redisTemplate.opsForZSet()
                .reverseRangeByScore(ChatRedisKey.MATCH_ROOM_LIST, 0, maxScore, 0, request.getLimit());
        
        if (matchIds == null || matchIds.isEmpty()) {
            return createEmptyResponse();
        }
        
        // 배치로 채팅방 정보 조회
        List<MatchChatRoomCreateResponse> rooms = batchGetMatchRooms(matchIds);
        
        // 다음 페이지 존재 여부 확인
        boolean hasMore = checkHasMoreWithoutKeyword(rooms, request.getLimit(), maxScore);
        String nextCursor = hasMore && !rooms.isEmpty() ? 
                rooms.get(rooms.size() - 1).getCreatedAt() : null;
        
        return MatchChatRoomListResponse.builder()
                .rooms(rooms)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(rooms.size())
                .build();
    }
    
    /**
     * 키워드로 매칭 채팅방 검색 (개선된 버전)
     */
    private MatchChatRoomListResponse searchMatchChatRoomsWithKeyword(MatchChatRoomListRequest request) {
        // 키워드 검색 시에는 더 많은 데이터를 가져와야 함 (필터링으로 인한 결과 부족 방지)
        long maxScore = Long.MAX_VALUE;
        if (request.getLastCreatedAt() != null) {
            maxScore = parseTimestampToScore(request.getLastCreatedAt()) - 1;
        }
        
        List<MatchChatRoomCreateResponse> rooms = new ArrayList<>();
        String nextCursor = null;
        boolean hasMore = false;
        
        // 점진적으로 데이터를 가져오면서 키워드 매칭
        int batchSize = Math.max(request.getLimit() * 3, 30); // 키워드 필터링을 고려한 배치 크기
        int offset = 0;
        int maxAttempts = 5; // 무한 루프 방지
        int attempts = 0;
        
        while (rooms.size() < request.getLimit() && attempts < maxAttempts) {
            Set<String> matchIds = redisTemplate.opsForZSet()
                    .reverseRangeByScore(ChatRedisKey.MATCH_ROOM_LIST, 0, maxScore, offset, batchSize);
            
            if (matchIds == null || matchIds.isEmpty()) {
                break; // 더 이상 데이터가 없음
            }
            
            // 현재 배치에서 키워드 매칭되는 채팅방들 찾기
            List<MatchChatRoomCreateResponse> batchRooms = searchInBatch(matchIds, request.getKeyword());
            
            for (MatchChatRoomCreateResponse room : batchRooms) {
                if (rooms.size() < request.getLimit()) {
                    rooms.add(room);
                    nextCursor = room.getCreatedAt();
                } else {
                    hasMore = true; // 더 가져올 데이터가 있음을 표시
                    break;
                }
            }
            
            // 다음 배치 준비
            if (matchIds.size() < batchSize) {
                break; // 마지막 배치였음
            }
            
            offset += batchSize;
            attempts++;
        }
        
        // hasMore가 아직 확정되지 않았다면 다음 데이터 존재 여부 확인
        if (!hasMore && !rooms.isEmpty()) {
            hasMore = checkHasMoreWithKeyword(nextCursor, request.getKeyword());
        }
        
        return MatchChatRoomListResponse.builder()
                .rooms(rooms)
                .nextCursor(hasMore ? nextCursor : null)
                .hasMore(hasMore)
                .count(rooms.size())
                .build();
    }
    
    /**
     * 배치 단위로 채팅방 정보 조회 (성능 최적화)
     */
    private List<MatchChatRoomCreateResponse> batchGetMatchRooms(Set<String> matchIds) {
        List<MatchChatRoomCreateResponse> rooms = new ArrayList<>();
        
        // Redis Pipeline 사용하여 배치 조회 최적화
        List<String> roomKeys = matchIds.stream()
                .map(ChatRedisKey::getMatchRoomInfoKey)
                .toList();
        
        // 배치로 모든 키 조회
        List<String> roomJsons = redisTemplate.opsForValue().multiGet(roomKeys);
        
        if (roomJsons != null) {
            for (String roomJson : roomJsons) {
                if (roomJson != null) {
                    try {
                        MatchChatRoom matchRoom = objectMapper.readValue(roomJson, MatchChatRoom.class);
                        // ACTIVE 상태의 채팅방만 포함
                        if ("ACTIVE".equals(matchRoom.getStatus())) {
                            rooms.add(convertToResponse(matchRoom));
                        }
                    } catch (Exception e) {
                        log.warn("매칭 채팅방 정보 파싱 실패", e);
                        // 개별 실패는 무시하고 계속 진행
                    }
                }
            }
        }
        
        return rooms;
    }
    
    /**
     * 배치 내에서 키워드 검색
     */
    private List<MatchChatRoomCreateResponse> searchInBatch(Set<String> matchIds, String keyword) {
        List<MatchChatRoomCreateResponse> matchedRooms = new ArrayList<>();
        
        // 배치로 채팅방 정보 조회
        List<MatchChatRoomCreateResponse> allRooms = batchGetMatchRooms(matchIds);
        
        // 키워드 필터링
        for (MatchChatRoomCreateResponse room : allRooms) {
            if (matchesKeywordOptimized(room, keyword)) {
                matchedRooms.add(room);
            }
        }
        
        return matchedRooms;
    }
    
    /**
     * 키워드 존재 여부 확인
     */
    private boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.trim().isEmpty();
    }
    
    /**
     * 키워드 없는 경우 다음 페이지 존재 여부 확인
     */
    private boolean checkHasMoreWithoutKeyword(List<MatchChatRoomCreateResponse> rooms, int limit, long currentMaxScore) {
        if (rooms.size() < limit) {
            return false; // 요청한 수보다 적게 나왔으면 더 이상 데이터 없음
        }
        
        // 마지막 항목 이후에 데이터가 더 있는지 확인
        if (!rooms.isEmpty()) {
            long lastScore = parseTimestampToScore(rooms.get(rooms.size() - 1).getCreatedAt()) - 1;
            Set<String> nextCheck = redisTemplate.opsForZSet()
                    .reverseRangeByScore(ChatRedisKey.MATCH_ROOM_LIST, 0, lastScore, 0, 1);
            return nextCheck != null && !nextCheck.isEmpty();
        }
        
        return false;
    }
    
    /**
     * 키워드 검색의 경우 다음 페이지 존재 여부 확인
     */
    private boolean checkHasMoreWithKeyword(String lastCreatedAt, String keyword) {
        if (lastCreatedAt == null) {
            return false;
        }
        
        long maxScore = parseTimestampToScore(lastCreatedAt) - 1;
        Set<String> nextBatch = redisTemplate.opsForZSet()
                .reverseRangeByScore(ChatRedisKey.MATCH_ROOM_LIST, 0, maxScore, 0, 10);
        
        if (nextBatch == null || nextBatch.isEmpty()) {
            return false;
        }
        
        // 다음 배치에서 키워드와 매칭되는 항목이 있는지 확인
        List<MatchChatRoomCreateResponse> nextRooms = searchInBatch(nextBatch, keyword);
        return !nextRooms.isEmpty();
    }
    
    /**
     * 빈 응답 생성
     */
    private MatchChatRoomListResponse createEmptyResponse() {
        return MatchChatRoomListResponse.builder()
                .rooms(new ArrayList<>())
                .nextCursor(null)
                .hasMore(false)
                .count(0)
                .build();
    }
    
    @Override
    public MatchChatRoomCreateResponse getMatchChatRoom(String matchId) {
        // 1. matchId 유효성 검증
        if (matchId == null || matchId.trim().isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
        
        try {
            String roomKey = ChatRedisKey.getMatchRoomInfoKey(matchId);
            String roomJson = redisTemplate.opsForValue().get(roomKey);
            
            if (roomJson == null) {
                return null; // Controller에서 ApiException으로 변환
            }
            
            MatchChatRoom matchRoom = objectMapper.readValue(roomJson, MatchChatRoom.class);
            
            // 2. 채팅방 상태 검증
            if (!"ACTIVE".equals(matchRoom.getStatus())) {
                throw new ApiException(ErrorCode.MATCH_CHAT_ROOM_CLOSED);
            }
            
            return convertToResponse(matchRoom);
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("매칭 채팅방 조회 실패 - matchId: {}", matchId, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }
    
    /**
     * MatchChatRoom을 MatchChatRoomResponse로 변환
     */
    private MatchChatRoomCreateResponse convertToResponse(MatchChatRoom matchRoom) {
        return MatchChatRoomCreateResponse.builder()
                .matchId(matchRoom.getMatchId())
                .gameId(matchRoom.getGameId())
                .matchTitle(matchRoom.getMatchTitle())
                .matchDescription(matchRoom.getMatchDescription())
                .teamId(matchRoom.getTeamId())
                .minAge(matchRoom.getMinAge())
                .maxAge(matchRoom.getMaxAge())
                .genderCondition(matchRoom.getGenderCondition())
                .maxParticipants(matchRoom.getMaxParticipants())
                .currentParticipants(matchRoom.getCurrentParticipants())
                .createdAt(matchRoom.getCreatedAt())
                .status(matchRoom.getStatus())
                .websocketUrl(String.format("ws://localhost:8084/ws/match-chat/websocket?matchId=%s", matchRoom.getMatchId()))
                .build();
    }
    
    /**
     * 키워드 매칭 검사 (기존 버전)
     */
    private boolean matchesKeyword(MatchChatRoom room, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return (room.getMatchTitle() != null && room.getMatchTitle().toLowerCase().contains(lowerKeyword)) ||
               (room.getMatchDescription() != null && room.getMatchDescription().toLowerCase().contains(lowerKeyword));
    }
    
    /**
     * 최적화된 키워드 매칭 검사 (Response 객체용)
     * 제목과 설명에서 동시에 키워드 검색
     */
    private boolean matchesKeywordOptimized(MatchChatRoomCreateResponse room, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        
        String lowerKeyword = keyword.toLowerCase().trim();
        
        // 제목에서 검색
        boolean titleMatch = room.getMatchTitle() != null && 
                room.getMatchTitle().toLowerCase().contains(lowerKeyword);
        
        // 설명에서 검색  
        boolean descriptionMatch = room.getMatchDescription() != null && 
                room.getMatchDescription().toLowerCase().contains(lowerKeyword);
        
        // 제목 또는 설명 중 하나라도 매칭되면 true
        return titleMatch || descriptionMatch;
    }
    
    /**
     * 경기 ID 기반으로 매칭 채팅방 ID 생성
     */
    private String generateMatchId(Long gameId) {
        // UUID를 사용해서 고유한 매칭 ID 생성
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        return String.format("match_%s_%s", gameId, uniqueId);
    }
    
    /**
     * 타임스탬프 문자열을 점수(long)로 변환
     */
    private long parseTimestampToScore(String timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp);
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.warn("타임스탬프 파싱 실패 - timestamp: {}", timestamp, e);
            return System.currentTimeMillis();
        }
    }
    
    /**
     * 게임 날짜 문자열을 기준으로 TTL 계산 (게임 날짜 자정까지)
     */
    private Duration calculateTTLFromGameDate(String gameDateStr) {
        try {
            LocalDate gameDate = LocalDate.parse(gameDateStr, DATE_FORMATTER);
            LocalDateTime midnightAfterGame = gameDate.plusDays(1).atTime(LocalTime.MIDNIGHT);
            
            Duration ttl = Duration.between(LocalDateTime.now(), midnightAfterGame);
            
            // TTL이 음수이거나 0에 가깝다면 기본 1시간 설정
            if (ttl.isNegative() || ttl.toMinutes() < 10) {
                ttl = Duration.ofHours(1);
            }
            
            log.info("게임 날짜: {}, TTL: {}시간", gameDate, ttl.toHours());
            return ttl;
        } catch (Exception e) {
            log.warn("게임 날짜 파싱 실패 - gameDateStr: {}", gameDateStr, e);
            return Duration.ofHours(1); // 기본값
        }
    }
    
}