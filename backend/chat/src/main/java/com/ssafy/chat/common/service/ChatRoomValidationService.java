package com.ssafy.chat.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.global.constants.ChatRedisKey;
import com.ssafy.chat.watch.service.WatchChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 채팅방 존재 여부 검증 서비스
 * REST API 단계에서 방 존재 여부를 확인하여 잘못된 입장 요청을 사전에 차단
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomValidationService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final WatchChatRoomService watchChatRoomService;
    private final ObjectMapper objectMapper;
    
    /**
     * 매칭 채팅방 존재 여부 확인
     * @param matchId 매칭 채팅방 ID
     * @return 존재하고 활성 상태면 true
     */
    public boolean existsMatchChatRoom(String matchId) {
        try {
            String roomKey = ChatRedisKey.getMatchRoomInfoKey(matchId);
            boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(roomKey));
            
            if (exists) {
                // 추가로 방 상태도 확인
                return isMatchRoomActive(matchId);
            }
            return false;
            
        } catch (Exception e) {
            log.error("매칭 채팅방 존재 여부 확인 실패 - matchId: {}", matchId, e);
            return false;
        }
    }
    
    /**
     * 관전 채팅방 존재 여부 확인
     * @param gameId 게임 ID
     * @param teamId 팀 ID
     * @return 존재하고 활성 상태면 true
     */
    public boolean existsWatchChatRoom(Long gameId, Long teamId) {
        try {
            return watchChatRoomService.existsWatchChatRoom(gameId, teamId);
        } catch (Exception e) {
            log.error("관전 채팅방 존재 여부 확인 실패 - gameId: {}, teamId: {}", gameId, teamId, e);
            return false;
        }
    }
    
    /**
     * roomId로 관전 채팅방 존재 여부 확인
     * @param roomId 채팅방 ID
     * @return 존재하면 true
     */
    public boolean existsWatchChatRoom(String roomId) {
        try {
            return watchChatRoomService.getWatchChatRoom(roomId) != null;
        } catch (Exception e) {
            log.error("관전 채팅방 존재 여부 확인 실패 - roomId: {}", roomId, e);
            return false;
        }
    }
    
    /**
     * 범용 채팅방 존재 여부 확인
     * roomId 패턴을 분석하여 적절한 검증 로직 호출
     * @param roomId 채팅방 ID
     * @return 존재하면 true
     */
    public boolean existsChatRoom(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return false;
        }
        
        try {
            if (roomId.startsWith("match_")) {
                return existsMatchChatRoom(roomId);
            } else if (roomId.startsWith("watch_")) {
                return existsWatchChatRoom(roomId);
            }
            
            log.warn("알 수 없는 채팅방 ID 패턴 - roomId: {}", roomId);
            return false;
            
        } catch (Exception e) {
            log.error("채팅방 존재 여부 확인 실패 - roomId: {}", roomId, e);
            return false;
        }
    }
    
    /**
     * 매칭 채팅방 활성 상태 확인
     * @param matchId 매칭 채팅방 ID
     * @return 활성 상태면 true
     */
    private boolean isMatchRoomActive(String matchId) {
        try {
            String roomKey = ChatRedisKey.getMatchRoomInfoKey(matchId);
            String roomJson = redisTemplate.opsForValue().get(roomKey);
            
            if (roomJson != null) {
                JsonNode roomNode = objectMapper.readTree(roomJson);
                String status = roomNode.get("status").asText();
                
                // ACTIVE 또는 WAITING 상태는 입장 가능
                return "ACTIVE".equals(status) || "WAITING".equals(status);
            }
            
        } catch (Exception e) {
            log.error("매칭 채팅방 상태 확인 실패 - matchId: {}", matchId, e);
        }
        return false;
    }
    
    /**
     * 매칭 채팅방이 가득 찼는지 확인
     * @param matchId 매칭 채팅방 ID
     * @return 가득 찼으면 true
     */
    public boolean isMatchRoomFull(String matchId) {
        try {
            String roomKey = ChatRedisKey.getMatchRoomInfoKey(matchId);
            String roomJson = redisTemplate.opsForValue().get(roomKey);
            
            if (roomJson != null) {
                JsonNode roomNode = objectMapper.readTree(roomJson);
                int currentParticipants = roomNode.get("currentParticipants").asInt();
                int maxParticipants = roomNode.get("maxParticipants").asInt();
                
                return currentParticipants >= maxParticipants;
            }
            
        } catch (Exception e) {
            log.error("매칭 채팅방 정원 확인 실패 - matchId: {}", matchId, e);
        }
        return true; // 에러 시 안전하게 가득 찬 것으로 처리
    }
    
    /**
     * 채팅방 기본 정보 조회
     * @param roomId 채팅방 ID
     * @return 채팅방 기본 정보 (타입, 게임ID 등)
     */
    public ChatRoomInfo getChatRoomInfo(String roomId) {
        if (roomId.startsWith("match_")) {
            return getMatchRoomInfo(roomId);
        } else if (roomId.startsWith("watch_")) {
            return getWatchRoomInfo(roomId);
        }
        return null;
    }
    
    private ChatRoomInfo getMatchRoomInfo(String matchId) {
        try {
            String roomKey = ChatRedisKey.getMatchRoomInfoKey(matchId);
            String roomJson = redisTemplate.opsForValue().get(roomKey);
            
            if (roomJson != null) {
                JsonNode roomNode = objectMapper.readTree(roomJson);
                return ChatRoomInfo.builder()
                    .roomId(matchId)
                    .roomType("MATCH")
                    .gameId(roomNode.get("gameId").asLong())
                    .title(roomNode.has("matchTitle") ? roomNode.get("matchTitle").asText() : "매칭 채팅")
                    .currentParticipants(roomNode.get("currentParticipants").asInt())
                    .maxParticipants(roomNode.get("maxParticipants").asInt())
                    .status(roomNode.get("status").asText())
                    .build();
            }
        } catch (Exception e) {
            log.error("매칭 채팅방 정보 조회 실패 - matchId: {}", matchId, e);
        }
        return null;
    }
    
    private ChatRoomInfo getWatchRoomInfo(String roomId) {
        try {
            var watchRoom = watchChatRoomService.getWatchChatRoom(roomId);
            if (watchRoom != null) {
                return ChatRoomInfo.builder()
                    .roomId(roomId)
                    .roomType("WATCH")
                    .gameId(watchRoom.getGameId())
                    .title(watchRoom.getRoomName())
                    .currentParticipants(watchRoom.getCurrentParticipants())
                    .maxParticipants(watchRoom.getMaxParticipants())
                    .status(watchRoom.getStatus())
                    .build();
            }
        } catch (Exception e) {
            log.error("관전 채팅방 정보 조회 실패 - roomId: {}", roomId, e);
        }
        return null;
    }
    
    /**
     * 채팅방 기본 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatRoomInfo {
        private String roomId;
        private String roomType;
        private Long gameId;
        private String title;
        private Integer currentParticipants;
        private Integer maxParticipants;
        private String status;
    }
}