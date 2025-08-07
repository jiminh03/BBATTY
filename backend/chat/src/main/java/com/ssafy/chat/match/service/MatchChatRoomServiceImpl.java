package com.ssafy.chat.match.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.match.dto.*;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.global.constants.ErrorCode;
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
    private final GameInfoService gameInfoService;
    private final ChatAuthRequestProducer chatAuthRequestProducer;
    private final ChatAuthResultService chatAuthResultService;
    private final JwtProvider jwtProvider;
    
    private static final String MATCH_ROOM_PREFIX = "match_room:";
    private static final String MATCH_ROOM_LIST_KEY = "match_rooms:list";
    
    @Override
    public MatchChatRoomCreateResponse createMatchChatRoom(MatchChatRoomCreateRequest request) {
        try {
            // 1. gameId 유효성 검증
            GameInfo gameInfo = validateGameId(request.getGameId());
            if (gameInfo == null) {
                throw new ApiException(ErrorCode.NOT_FOUND, "존재하지 않는 경기입니다.");
            }
            
            // 2. 경기 ID 기반으로 매칭 채팅방 ID 자동 생성
            String matchId = generateMatchId(request.getGameId());
            
            // 매칭방 생성
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
                    .ownerId("dummy_user_123") // TODO: 실제 사용자 ID로 변경
                    .build();
            
            // 3. TTL 계산 (경기 날짜 자정까지)
            Duration ttl = calculateTTL(gameInfo.getDateTime());
            
            // 4. Redis에 TTL과 함께 저장
            String roomKey = MATCH_ROOM_PREFIX + matchId;
            String roomJson = objectMapper.writeValueAsString(matchRoom);
            redisTemplate.opsForValue().set(roomKey, roomJson, ttl);
            
            // 5. 매칭방 목록에도 TTL과 함께 추가
            long score = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(MATCH_ROOM_LIST_KEY, matchId, score);
            redisTemplate.expire(MATCH_ROOM_LIST_KEY, ttl);
            
            log.info("매칭 채팅방 생성 완료 - gameId: {}, matchId: {}", request.getGameId(), matchId);
            
            return convertToResponse(matchRoom);
            
        } catch (ApiException e) {
            throw e; // ApiException은 그대로 다시 던지기
        } catch (JsonProcessingException e) {
            log.error("매칭 채팅방 직렬화 실패 - gameId: {}", request.getGameId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "매칭 채팅방 생성에 실패했습니다.");
        } catch (Exception e) {
            log.error("매칭 채팅방 생성 실패 - gameId: {}", request.getGameId(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "매칭 채팅방 생성에 실패했습니다.");
        }
    }
    
    @Override
    public MatchChatRoomListResponse getMatchChatRoomList(MatchChatRoomListRequest request) {
        try {
            // 클라이언트가 제공한 cursor(마지막으로 받은 생성시간) 이전 데이터 조회
            long maxScore = Long.MAX_VALUE;
            if (request.getLastCreatedAt() != null) {
                maxScore = parseTimestampToScore(request.getLastCreatedAt()) - 1; // 이전 데이터만 가져오기 위해 -1
            }
            
            // Redis sorted set에서 최신순으로 데이터 조회
            Set<String> matchIds = redisTemplate.opsForZSet()
                    .reverseRangeByScore(MATCH_ROOM_LIST_KEY, 0, maxScore, 0, request.getLimit() + 10); // 필터링을 위해 여유분 추가
            
            if (matchIds == null || matchIds.isEmpty()) {
                return MatchChatRoomListResponse.builder()
                        .rooms(new ArrayList<>())
                        .nextCursor(null)
                        .hasMore(false)
                        .count(0)
                        .build();
            }
            
            // 각 매칭방 정보 조회 및 필터링
            List<MatchChatRoomCreateResponse> rooms = new ArrayList<>();
            String nextCursor = null;
            
            for (String matchId : matchIds) {
                // limit에 도달하면 중단
                if (rooms.size() >= request.getLimit()) {
                    break;
                }
                
                String roomKey = MATCH_ROOM_PREFIX + matchId;
                String roomJson = redisTemplate.opsForValue().get(roomKey);
                
                if (roomJson != null) {
                    MatchChatRoom matchRoom = objectMapper.readValue(roomJson, MatchChatRoom.class);
                    
                    // 검색 키워드 필터링
                    if (matchesKeyword(matchRoom, request.getKeyword())) {
                        rooms.add(convertToResponse(matchRoom));
                        nextCursor = matchRoom.getCreatedAt(); // 마지막 항목의 생성시간
                    }
                }
            }
            
            // 더 가져올 데이터가 있는지 체크
            boolean hasMore = false;
            if (rooms.size() == request.getLimit()) {
                // 다음 페이지 데이터가 있는지 확인
                long nextMaxScore = nextCursor != null ? parseTimestampToScore(nextCursor) - 1 : 0;
                Set<String> nextPageCheck = redisTemplate.opsForZSet()
                        .reverseRangeByScore(MATCH_ROOM_LIST_KEY, 0, nextMaxScore, 0, 1);
                hasMore = nextPageCheck != null && !nextPageCheck.isEmpty();
            }
            
            return MatchChatRoomListResponse.builder()
                    .rooms(rooms)
                    .nextCursor(hasMore ? nextCursor : null)
                    .hasMore(hasMore)
                    .count(rooms.size())
                    .build();
            
        } catch (Exception e) {
            log.error("매칭 채팅방 목록 조회 실패", e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "매칭 채팅방 목록 조회에 실패했습니다.");
        }
    }
    
    @Override
    public MatchChatRoomCreateResponse getMatchChatRoom(String matchId) {
        // 1. matchId 유효성 검증
        if (matchId == null || matchId.trim().isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "매칭 채팅방 ID가 비어있습니다.");
        }
        
        try {
            String roomKey = MATCH_ROOM_PREFIX + matchId;
            String roomJson = redisTemplate.opsForValue().get(roomKey);
            
            if (roomJson == null) {
                return null; // Controller에서 ApiException으로 변환
            }
            
            MatchChatRoom matchRoom = objectMapper.readValue(roomJson, MatchChatRoom.class);
            
            // 2. 채팅방 상태 검증
            if (!"ACTIVE".equals(matchRoom.getStatus())) {
                throw new ApiException(ErrorCode.MATCH_CHAT_ROOM_CLOSED, "비활성 상태의 매칭 채팅방입니다.");
            }
            
            return convertToResponse(matchRoom);
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("매칭 채팅방 조회 실패 - matchId: {}", matchId, e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "매칭 채팅방 조회에 실패했습니다.");
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
     * 키워드 매칭 검사
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
     * gameId 유효성 검증
     */
    private GameInfo validateGameId(Long gameId) {
        try {
            
            // 모든 가능한 날짜에서 해당 gameId 검색 (2주간)
            LocalDate startDate = LocalDate.now().minusDays(1);
            LocalDate endDate = LocalDate.now().plusDays(14);
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                List<GameInfo> games = gameInfoService.getGameInfosByDate(dateStr);
                
                for (GameInfo game : games) {
                    if (game.getGameId().equals(gameId)) {
                        return game;
                    }
                }
            }
            
            return null; // 찾지 못함
        } catch (Exception e) {
            log.warn("gameId 검증 중 오류: {}", gameId, e);
            return null;
        }
    }
    
    /**
     * 경기 날짜를 기준으로 TTL 계산 (경기 날짜 자정까지)
     */
    private Duration calculateTTL(LocalDateTime gameDateTime) {
        LocalDate gameDate = gameDateTime.toLocalDate();
        LocalDateTime midnightAfterGame = gameDate.plusDays(1).atTime(LocalTime.MIDNIGHT);
        
        Duration ttl = Duration.between(LocalDateTime.now(), midnightAfterGame);
        
        // TTL이 음수이거나 0에 가깝다면 기본 1시간 설정
        if (ttl.isNegative() || ttl.toMinutes() < 10) {
            ttl = Duration.ofHours(1);
        }
        
        log.info("경기 날짜: {}, TTL: {}시간", gameDate, ttl.toHours());
        return ttl;
    }
}