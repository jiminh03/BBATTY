package com.ssafy.chat.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.SessionInfo;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.config.ChatConfiguration;
import com.ssafy.chat.global.constants.ChatRedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 분산 환경에서 WebSocket 세션을 관리하는 서비스 구현체
 * Redis를 통한 중앙화된 세션 정보 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedSessionManagerServiceImpl implements DistributedSessionManagerService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisUtil redisUtil;
    private final ChatConfiguration chatConfiguration;
    private final ObjectMapper objectMapper;
    
    // 로컬 WebSocket 세션 캐시 (빠른 접근을 위해)
    private final Map<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();
    
    // Redis 키 패턴
    private static final String SESSION_INFO_KEY = "distributed:session:info:";
    private static final String ROOM_SESSIONS_KEY = "distributed:room:sessions:";
    private static final String INSTANCE_SESSIONS_KEY = "distributed:instance:sessions:";
    private static final String SESSION_HEARTBEAT_KEY = "distributed:session:heartbeat:";
    private static final String BROADCAST_CHANNEL = "distributed:broadcast:";
    
    @Override
    public void registerSession(String roomId, WebSocketSession session, SessionInfo sessionInfo) {
        try {
            String sessionId = session.getId();
            String instanceId = chatConfiguration.getOrGenerateInstanceId();
            
            // 1. 로컬 세션 캐시에 저장
            localSessions.put(sessionId, session);
            
            // 2. Redis에 세션 정보 저장
            String sessionInfoKey = SESSION_INFO_KEY + sessionId;
            redisUtil.setValue(sessionInfoKey, sessionInfo, chatConfiguration.getDistributedSessionTtl());
            
            // 3. 채팅방별 세션 목록에 추가
            String roomSessionsKey = ROOM_SESSIONS_KEY + roomId;
            redisTemplate.opsForSet().add(roomSessionsKey, sessionId);
            redisTemplate.expire(roomSessionsKey, chatConfiguration.getDistributedSessionTtl());
            
            // 4. 인스턴스별 세션 목록에 추가
            String instanceSessionsKey = INSTANCE_SESSIONS_KEY + instanceId;
            redisTemplate.opsForSet().add(instanceSessionsKey, sessionId);
            redisTemplate.expire(instanceSessionsKey, chatConfiguration.getDistributedSessionTtl());
            
            // 5. 세션 하트비트 초기화
            updateSessionHeartbeat(sessionId);
            
            log.info("분산 세션 등록 완료 - roomId: {}, sessionId: {}, instanceId: {}", 
                    roomId, sessionId, instanceId);
            
        } catch (Exception e) {
            log.error("분산 세션 등록 실패 - roomId: {}, sessionId: {}", 
                    roomId, session.getId(), e);
        }
    }
    
    @Override
    public void unregisterSession(String roomId, String sessionId) {
        try {
            String instanceId = chatConfiguration.getOrGenerateInstanceId();
            
            // 1. 로컬 세션 캐시에서 제거
            localSessions.remove(sessionId);
            
            // 2. Redis에서 세션 정보 삭제
            String sessionInfoKey = SESSION_INFO_KEY + sessionId;
            redisUtil.deleteKey(sessionInfoKey);
            
            // 3. 채팅방별 세션 목록에서 제거
            String roomSessionsKey = ROOM_SESSIONS_KEY + roomId;
            redisTemplate.opsForSet().remove(roomSessionsKey, sessionId);
            
            // 4. 인스턴스별 세션 목록에서 제거
            String instanceSessionsKey = INSTANCE_SESSIONS_KEY + instanceId;
            redisTemplate.opsForSet().remove(instanceSessionsKey, sessionId);
            
            // 5. 세션 하트비트 정보 삭제
            String heartbeatKey = SESSION_HEARTBEAT_KEY + sessionId;
            redisUtil.deleteKey(heartbeatKey);
            
            log.info("분산 세션 해제 완료 - roomId: {}, sessionId: {}", roomId, sessionId);
            
        } catch (Exception e) {
            log.error("분산 세션 해제 실패 - roomId: {}, sessionId: {}", roomId, sessionId, e);
        }
    }
    
    @Override
    public void unregisterSessionFromAllRooms(String sessionId) {
        try {
            // 먼저 이 세션이 속한 모든 채팅방을 찾아야 함
            Set<String> activeRooms = getActiveRooms();
            
            for (String roomId : activeRooms) {
                String roomSessionsKey = ROOM_SESSIONS_KEY + roomId;
                Boolean isMember = redisTemplate.opsForSet().isMember(roomSessionsKey, sessionId);
                if (Boolean.TRUE.equals(isMember)) {
                    unregisterSession(roomId, sessionId);
                }
            }
            
        } catch (Exception e) {
            log.error("모든 채팅방에서 세션 해제 실패 - sessionId: {}", sessionId, e);
        }
    }
    
    @Override
    public List<SessionInfo> getActiveSessionsInRoom(String roomId) {
        try {
            String roomSessionsKey = ROOM_SESSIONS_KEY + roomId;
            Set<Object> sessionIdObjects = redisTemplate.opsForSet().members(roomSessionsKey);
            Set<String> sessionIds = sessionIdObjects != null ? 
                sessionIdObjects.stream().map(Object::toString).collect(Collectors.toSet()) : 
                Collections.emptySet();
            
            if (sessionIds == null || sessionIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<SessionInfo> sessionInfos = new ArrayList<>();
            for (String sessionId : sessionIds) {
                String sessionInfoKey = SESSION_INFO_KEY + sessionId;
                Object sessionData = redisUtil.getValue(sessionInfoKey);
                if (sessionData != null) {
                    SessionInfo sessionInfo = objectMapper.convertValue(sessionData, SessionInfo.class);
                    if (sessionInfo != null && sessionInfo.isValid()) {
                        sessionInfos.add(sessionInfo);
                    }
                }
            }
            
            return sessionInfos;
            
        } catch (Exception e) {
            log.error("채팅방 활성 세션 조회 실패 - roomId: {}", roomId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public int getActiveSessionCount(String roomId) {
        try {
            String roomSessionsKey = ROOM_SESSIONS_KEY + roomId;
            Long count = redisTemplate.opsForSet().size(roomSessionsKey);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("채팅방 활성 세션 수 조회 실패 - roomId: {}", roomId, e);
            return 0;
        }
    }
    
    @Override
    public List<SessionInfo> getActiveSessionsByInstance(String instanceId) {
        try {
            String instanceSessionsKey = INSTANCE_SESSIONS_KEY + instanceId;
            Set<Object> sessionIdObjects = redisTemplate.opsForSet().members(instanceSessionsKey);
            Set<String> sessionIds = sessionIdObjects != null ? 
                sessionIdObjects.stream().map(Object::toString).collect(Collectors.toSet()) : 
                Collections.emptySet();
            
            if (sessionIds == null || sessionIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<SessionInfo> sessionInfos = new ArrayList<>();
            for (String sessionId : sessionIds) {
                String sessionInfoKey = SESSION_INFO_KEY + sessionId;
                Object sessionData = redisUtil.getValue(sessionInfoKey);
                if (sessionData != null) {
                    SessionInfo sessionInfo = objectMapper.convertValue(sessionData, SessionInfo.class);
                    if (sessionInfo != null && sessionInfo.isValid()) {
                        sessionInfos.add(sessionInfo);
                    }
                }
            }
            
            return sessionInfos;
            
        } catch (Exception e) {
            log.error("인스턴스 활성 세션 조회 실패 - instanceId: {}", instanceId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Set<String> getActiveRooms() {
        try {
            Set<String> roomKeys = redisUtil.scanKeys(ROOM_SESSIONS_KEY + "*");
            return roomKeys.stream()
                    .map(key -> key.replace(ROOM_SESSIONS_KEY, ""))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("활성 채팅방 목록 조회 실패", e);
            return Collections.emptySet();
        }
    }
    
    @Override
    public void updateSessionHeartbeat(String sessionId) {
        try {
            String heartbeatKey = SESSION_HEARTBEAT_KEY + sessionId;
            redisUtil.setValue(heartbeatKey, System.currentTimeMillis(), 
                             chatConfiguration.getDistributedSessionTtl());
        } catch (Exception e) {
            log.error("세션 하트비트 업데이트 실패 - sessionId: {}", sessionId, e);
        }
    }
    
    @Override
    public int cleanupInactiveSessions() {
        try {
            Set<String> sessionKeys = redisUtil.scanKeys(SESSION_HEARTBEAT_KEY + "*");
            int cleanedCount = 0;
            long cutoffTime = System.currentTimeMillis() - 
                            chatConfiguration.getInactiveSessionThreshold().toMillis();
            
            for (String heartbeatKey : sessionKeys) {
                Object heartbeatData = redisUtil.getValue(heartbeatKey);
                if (heartbeatData != null) {
                    long lastHeartbeat = ((Number) heartbeatData).longValue();
                    if (lastHeartbeat < cutoffTime) {
                        String sessionId = heartbeatKey.replace(SESSION_HEARTBEAT_KEY, "");
                        unregisterSessionFromAllRooms(sessionId);
                        cleanedCount++;
                    }
                }
            }
            
            if (cleanedCount > 0) {
                log.info("비활성 세션 정리 완료 - 정리된 세션 수: {}", cleanedCount);
            }
            
            return cleanedCount;
            
        } catch (Exception e) {
            log.error("비활성 세션 정리 실패", e);
            return 0;
        }
    }
    
    @Override
    public int cleanupInstanceSessions(String instanceId) {
        try {
            String instanceSessionsKey = INSTANCE_SESSIONS_KEY + instanceId;
            Set<Object> sessionIdObjects = redisTemplate.opsForSet().members(instanceSessionsKey);
            Set<String> sessionIds = sessionIdObjects != null ? 
                sessionIdObjects.stream().map(Object::toString).collect(Collectors.toSet()) : 
                Collections.emptySet();
            
            if (sessionIds == null || sessionIds.isEmpty()) {
                return 0;
            }
            
            for (String sessionId : sessionIds) {
                unregisterSessionFromAllRooms(sessionId);
            }
            
            // 인스턴스 세션 목록도 삭제
            redisUtil.deleteKey(instanceSessionsKey);
            
            log.info("인스턴스 세션 정리 완료 - instanceId: {}, 정리된 세션 수: {}", 
                    instanceId, sessionIds.size());
            
            return sessionIds.size();
            
        } catch (Exception e) {
            log.error("인스턴스 세션 정리 실패 - instanceId: {}", instanceId, e);
            return 0;
        }
    }
    
    @Override
    public void broadcastToRoom(String roomId, String message, String excludeInstanceId) {
        try {
            // Redis Pub/Sub을 통한 다른 인스턴스에 브로드캐스트
            String channelName = BROADCAST_CHANNEL + roomId;
            Map<String, Object> broadcastData = Map.of(
                    "roomId", roomId,
                    "message", message,
                    "sourceInstanceId", chatConfiguration.getOrGenerateInstanceId(),
                    "excludeInstanceId", excludeInstanceId != null ? excludeInstanceId : "",
                    "timestamp", System.currentTimeMillis()
            );
            
            redisTemplate.convertAndSend(channelName, broadcastData);
            
            // 로컬 세션에도 직접 전송
            sendToLocalSessionsInRoom(roomId, message);
            
        } catch (Exception e) {
            log.error("채팅방 브로드캐스트 실패 - roomId: {}", roomId, e);
        }
    }
    
    @Override
    public void sendToSession(String sessionId, String message) {
        try {
            WebSocketSession session = localSessions.get(sessionId);
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(message));
                log.debug("세션 메시지 전송 성공 - sessionId: {}", sessionId);
            } else {
                log.warn("세션을 찾을 수 없거나 연결이 닫힘 - sessionId: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("세션 메시지 전송 실패 - sessionId: {}", sessionId, e);
        }
    }
    
    @Override
    public int getTotalActiveSessionCount() {
        try {
            Set<String> sessionKeys = redisUtil.scanKeys(SESSION_INFO_KEY + "*");
            return sessionKeys.size();
        } catch (Exception e) {
            log.error("전체 활성 세션 수 조회 실패", e);
            return 0;
        }
    }
    
    @Override
    public int getTotalActiveRoomCount() {
        return getActiveRooms().size();
    }
    
    @Override
    public Map<String, Integer> getSessionDistributionByInstance() {
        try {
            Set<String> instanceKeys = redisUtil.scanKeys(INSTANCE_SESSIONS_KEY + "*");
            Map<String, Integer> distribution = new HashMap<>();
            
            for (String instanceKey : instanceKeys) {
                String instanceId = instanceKey.replace(INSTANCE_SESSIONS_KEY, "");
                Long sessionCount = redisTemplate.opsForSet().size(instanceKey);
                distribution.put(instanceId, sessionCount != null ? sessionCount.intValue() : 0);
            }
            
            return distribution;
            
        } catch (Exception e) {
            log.error("인스턴스별 세션 분포 조회 실패", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * 로컬 세션에 메시지 전송 (내부 메서드)
     */
    private void sendToLocalSessionsInRoom(String roomId, String message) {
        try {
            // 방의 모든 세션 ID 가져오기
            String roomSessionsKey = ROOM_SESSIONS_KEY + roomId;
            Set<Object> sessionIds = redisTemplate.opsForSet().members(roomSessionsKey);
            
            if (sessionIds != null) {
                for (Object sessionIdObj : sessionIds) {
                    String sessionId = sessionIdObj.toString();
                    WebSocketSession session = localSessions.get(sessionId);
                    if (session != null && session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        log.debug("메시지 전송 완료 - sessionId: {}, roomId: {}", sessionId, roomId);
                    }
                }
                
                log.debug("방 브로드캐스트 완료 - roomId: {}, 전송된 세션 수: {}", roomId, sessionIds.size());
            }
            
        } catch (Exception e) {
            log.error("로컬 세션 메시지 전송 실패 - roomId: {}", roomId, e);
        }
    }
}