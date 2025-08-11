package com.ssafy.chat.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.util.ChatRoomUtils;
import com.ssafy.chat.config.ChatConfiguration;
import com.ssafy.chat.global.constants.ChatRedisKey;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.MatchChatRoom;
import com.ssafy.chat.match.dto.MatchChatRoomListRequest;
import com.ssafy.chat.match.dto.MatchChatRoomListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 매칭 채팅방 조회 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class
MatchChatRoomQueryServiceImpl implements MatchChatRoomQueryService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatConfiguration chatConfiguration;
    private final ChatRoomUtils chatRoomUtils;
    
    @Override
    public MatchChatRoomListResponse getMatchChatRoomList(MatchChatRoomListRequest request) {
        try {
            // 키워드가 있는 경우와 없는 경우를 다르게 처리
            if (hasKeyword(request.getKeyword())) {
                return searchMatchChatRooms(request);
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
    
    @Override
    public MatchChatRoomListResponse searchMatchChatRooms(MatchChatRoomListRequest request) {
        try {
            log.info("매칭 채팅방 키워드 검색 - keyword: {}", request.getKeyword());
            
            // 키워드 검색은 더 많은 데이터를 가져와서 필터링
            long maxScore = Long.MAX_VALUE;
            if (request.getLastCreatedAt() != null) {
                maxScore = chatRoomUtils.parseTimestampToScore(request.getLastCreatedAt()) - 1;
            }
            
            // 키워드 필터링을 위해 더 많이 조회
            int fetchLimit = Math.min(request.getLimit() * 3, chatConfiguration.getMaxMatchChatRoomPageSize());
            
            Set<String> matchIds = redisTemplate.opsForZSet()
                    .reverseRangeByScore(ChatRedisKey.MATCH_ROOM_LIST, 0, maxScore, 0, fetchLimit);
            
            if (matchIds == null || matchIds.isEmpty()) {
                return createEmptyResponse();
            }
            
            // Redis에서 채팅방 정보 조회 후 키워드 필터링
            List<MatchChatRoom> filteredRooms = matchIds.stream()
                    .map(this::fetchChatRoomFromRedis)
                    .filter(room -> room != null && matchesKeyword(room, request.getKeyword()))
                    .limit(request.getLimit())
                    .collect(Collectors.toList());
            
            boolean hasMore = filteredRooms.size() >= request.getLimit();
            String nextCursor = hasMore && !filteredRooms.isEmpty() ? 
                    filteredRooms.get(filteredRooms.size() - 1).getCreatedAt() : null;
            
            return MatchChatRoomListResponse.builder()
                    .chatRooms(filteredRooms)
                    .hasMore(hasMore)
                    .nextCursor(nextCursor)
                    .totalCount(filteredRooms.size())
                    .build();
            
        } catch (Exception e) {
            log.error("매칭 채팅방 검색 실패 - keyword: {}", request.getKeyword(), e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }
    
    @Override
    public MatchChatRoom getMatchChatRoomDetail(String matchId) {
        try {
            MatchChatRoom room = fetchChatRoomFromRedis(matchId);
            if (room == null) {
                throw new ApiException(ErrorCode.MATCH_CHAT_ROOM_NOT_FOUND);
            }
            
            return room;
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("매칭 채팅방 상세 조회 실패 - matchId: {}", matchId, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }
    
    @Override
    public MatchChatRoomListResponse getJoinableChatRooms(
            MatchChatRoomListRequest request, 
            Long userId, 
            Integer userAge, 
            String userGender) {
        
        try {
            // 기본 목록 조회
            MatchChatRoomListResponse baseResponse = getMatchChatRoomList(request);
            
            // 참여 가능한 채팅방만 필터링
            List<MatchChatRoom> joinableRooms = baseResponse.getChatRooms().stream()
                    .filter(room -> canUserJoinRoom(room, userId, userAge, userGender))
                    .collect(Collectors.toList());
            
            return MatchChatRoomListResponse.builder()
                    .chatRooms(joinableRooms)
                    .hasMore(baseResponse.isHasMore())
                    .nextCursor(baseResponse.getNextCursor())
                    .totalCount(joinableRooms.size())
                    .build();
            
        } catch (Exception e) {
            log.error("참여 가능한 채팅방 조회 실패 - userId: {}", userId, e);
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
            maxScore = chatRoomUtils.parseTimestampToScore(request.getLastCreatedAt()) - 1;
        }
        
        // 정확히 필요한 수만큼 조회 (키워드 필터링이 없으므로)
        Set<String> matchIds = redisTemplate.opsForZSet()
                .reverseRangeByScore(ChatRedisKey.MATCH_ROOM_LIST, 0, maxScore, 0, request.getLimit());
        
        if (matchIds == null || matchIds.isEmpty()) {
            return createEmptyResponse();
        }
        
        // Redis에서 채팅방 정보 조회
        List<MatchChatRoom> chatRooms = matchIds.stream()
                .map(this::fetchChatRoomFromRedis)
                .filter(room -> room != null)
                .collect(Collectors.toList());
        
        boolean hasMore = chatRooms.size() >= request.getLimit();
        String nextCursor = hasMore && !chatRooms.isEmpty() ? 
                chatRooms.get(chatRooms.size() - 1).getCreatedAt() : null;
        
        return MatchChatRoomListResponse.builder()
                .chatRooms(chatRooms)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .totalCount(chatRooms.size())
                .build();
    }
    
    /**
     * Redis에서 채팅방 정보 조회
     */
    private MatchChatRoom fetchChatRoomFromRedis(String matchId) {
        try {
            String roomKey = ChatRedisKey.getMatchRoomInfoKey(matchId);
            String roomJson = redisTemplate.opsForValue().get(roomKey);
            
            if (roomJson == null) {
                log.warn("매칭 채팅방 정보 없음 - matchId: {}", matchId);
                return null;
            }
            
            return objectMapper.readValue(roomJson, MatchChatRoom.class);
            
        } catch (Exception e) {
            log.error("매칭 채팅방 정보 조회 실패 - matchId: {}", matchId, e);
            return null;
        }
    }
    
    /**
     * 키워드 매칭 확인
     */
    private boolean matchesKeyword(MatchChatRoom room, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        
        String lowerKeyword = keyword.toLowerCase().trim();
        
        // 제목, 생성자 닉네임에서 키워드 검색
        return (room.getMatchTitle() != null && room.getMatchTitle().toLowerCase().contains(lowerKeyword))
                || (room.getCreatorNickname() != null && room.getCreatorNickname().toLowerCase().contains(lowerKeyword));
    }
    
    /**
     * 사용자가 채팅방에 참여할 수 있는지 확인
     */
    private boolean canUserJoinRoom(MatchChatRoom room, Long userId, Integer userAge, String userGender) {
        // 성별 제한 확인
        if (room.getGenderCondition() != null && 
            !room.getGenderCondition().equals("ALL") && 
            !room.getGenderCondition().equals(userGender)) {
            return false;
        }
        
        // 나이 제한 확인
        if (room.getMinAge() > 0 && room.getMaxAge() > 0 && userAge != null) {
            if (userAge < room.getMinAge() || userAge > room.getMaxAge()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 키워드 존재 여부 확인
     */
    private boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.trim().isEmpty();
    }
    
    /**
     * 빈 응답 생성
     */
    private MatchChatRoomListResponse createEmptyResponse() {
        return MatchChatRoomListResponse.builder()
                .chatRooms(new ArrayList<>())
                .hasMore(false)
                .nextCursor(null)
                .totalCount(0)
                .build();
    }
    
}