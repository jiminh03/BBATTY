package com.ssafy.bbatty.domain.chat.match.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.chat.common.handler.BaseChatWebSocketHandler;
import com.ssafy.bbatty.domain.chat.common.service.RedisPubSubService;
import com.ssafy.bbatty.domain.chat.match.service.MatchChatRoomService;
import com.ssafy.bbatty.domain.chat.match.service.MatchChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 매칭 채팅 WebSocket 핸들러
 * 빠른 매칭, 임시 채팅방, 매칭 상태 연동 등 매칭 채팅 특화 기능 제공
 */
@Component("matchChatWebSocketHandler")
@Slf4j
public class MatchChatWebSocketHandler extends BaseChatWebSocketHandler {

    private final MatchChatRoomService matchChatRoomService;
    private final MatchChatSessionService matchChatSessionService;
    private final RedisPubSubService redisPubSubService;

    // 매칭 채팅방 자동 정리를 위한 스케줄러
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Autowired
    public MatchChatWebSocketHandler(ObjectMapper objectMapper,
                                     MatchChatRoomService matchChatRoomService,
                                     MatchChatSessionService matchChatSessionService,
                                     RedisPubSubService redisPubSubService) {
        super(objectMapper);
        this.matchChatRoomService = matchChatRoomService;
        this.matchChatSessionService = matchChatSessionService;
        this.redisPubSubService = redisPubSubService;

        // 매칭 채팅방 정리 스케줄러 시작
        startMatchCleanupScheduler();
    }

    @Override
    protected UserSessionInfo createUserSessionInfo(WebSocketSession session) {
        String matchId = extractMatchId(session);
        String userId = extractUserId(session);
        String userName = extractUserName(session, userId);

        UserSessionInfo userInfo = new UserSessionInfo(userId, userName, matchId);

        // 매칭 채팅 특화 정보 추가
        userInfo.addAdditionalInfo("chatType", "match");
        userInfo.addAdditionalInfo("matchId", matchId);
        userInfo.addAdditionalInfo("matchStatus", "waiting"); // waiting, matched, timeout
        userInfo.addAdditionalInfo("maxWaitTime", getMaxWaitTime()); // 최대 대기 시간

        return userInfo;
    }

    @Override
    protected boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String matchId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            // 매칭 채팅방 존재 여부 및 상태 확인
            if (!matchChatRoomService.isMatchRoomActive(matchId)) {
                log.warn("비활성화된 매칭 채팅방 입장 시도 - matchId: {}, userId: {}", matchId, userId);
                return false;
            }

            // 매칭 채팅방 정원 확인 (보통 2-10명 제한)
            Long currentUsers = matchChatRoomService.getParticipantCount(matchId);
            Integer maxUsers = matchChatRoomService.getMaxParticipants(matchId);

            if (currentUsers >= maxUsers) {
                log.warn("매칭 채팅방 정원 초과 - matchId: {}, current: {}, max: {}", matchId, currentUsers, maxUsers);
                return false;
            }

            // 중복 참여 방지
            if (matchChatRoomService.isUserAlreadyInMatch(matchId, userId)) {
                log.warn("이미 참여 중인 매칭 - matchId: {}, userId: {}", matchId, userId);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("매칭 채팅방 입장 검증 실패 - userId: {}", userInfo.getUserId(), e);
            return false;
        }
    }

    @Override
    protected void handleUserJoin(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String matchId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            // 매칭 세션에 사용자 추가
            matchChatSessionService.addParticipant(matchId, userId, userInfo.getUserName());

            // 매칭 상태 확인 및 업데이트
            checkAndUpdateMatchStatus(matchId);

            // Redis Pub/Sub으로 다른 서버에 참여 알림
            Map<String, Object> joinEvent = createMatchJoinEvent(userInfo);
            redisPubSubService.publishMessage(matchId, joinEvent);

            // 자동 타임아웃 스케줄링
            scheduleMatchTimeout(matchId, userInfo);

            log.info("매칭 채팅방 입장 완료 - matchId: {}, userId: {}", matchId, userId);

        } catch (Exception e) {
            log.error("매칭 채팅방 입장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected void handleUserLeave(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String matchId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            // 매칭 세션에서 사용자 제거
            matchChatSessionService.removeParticipant(matchId, userId);

            // 매칭 상태 재확인
            checkAndUpdateMatchStatus(matchId);

            // Redis Pub/Sub으로 다른 서버에 퇴장 알림
            Map<String, Object> leaveEvent = createMatchLeaveEvent(userInfo);
            redisPubSubService.publishMessage(matchId, leaveEvent);

            log.info("매칭 채팅방 퇴장 완료 - matchId: {}, userId: {}", matchId, userId);

        } catch (Exception e) {
            log.error("매칭 채팅방 퇴장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected Map<String, Object> handleDomainMessage(WebSocketSession session, UserSessionInfo userInfo, String content) {
        try {
            String matchId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            // 매칭 완료된 상태에서는 메시지 제한
            String matchStatus = matchChatSessionService.getMatchStatus(matchId);
            if ("completed".equals(matchStatus)) {
                return createSystemMessage("매칭이 완료되어 더 이상 메시지를 보낼 수 없습니다.");
            }

            // 매칭 채팅 메시지 생성
            Map<String, Object> matchMessage = createMatchChatMessage(userInfo, content);

            // Redis Pub/Sub으로 다른 서버에 메시지 전파
            redisPubSubService.publishMessage(matchId, matchMessage);

            // 사용자 활동 시간 업데이트
            matchChatSessionService.updateParticipantActivity(matchId, userId);

            return matchMessage;

        } catch (Exception e) {
            log.error("매칭 채팅 메시지 처리 실패 - userId: {}", userInfo.getUserId(), e);
            return createErrorMessage("메시지 전송에 실패했습니다.");
        }
    }

    @Override
    protected boolean isValidMessage(String content, UserSessionInfo userInfo) {
        // 매칭 채팅은 더 짧은 메시지 제한
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        if (content.length() > 200) { // 최대 200자
            return false;
        }

        // 매칭 상태에 따른 메시지 제한
        String matchStatus = (String) userInfo.getAdditionalInfo().get("matchStatus");
        if ("completed".equals(matchStatus) || "timeout".equals(matchStatus)) {
            return false;
        }

        // 빠른 연속 메시지 방지 (매칭 채팅은 더 엄격)
        return !isTooFrequentMessage(userInfo);
    }

    /**
     * 매칭 상태 확인 및 업데이트
     */
    private void checkAndUpdateMatchStatus(String matchId) {
        try {
            Long participantCount = matchChatSessionService.getParticipantCount(matchId);
            Integer requiredCount = matchChatRoomService.getRequiredParticipants(matchId);

            if (participantCount >= requiredCount) {
                // 매칭 완료!
                completeMatch(matchId);
            } else {
                // 매칭 대기 중 상태 업데이트
                updateMatchWaitingStatus(matchId, participantCount, requiredCount);
            }

        } catch (Exception e) {
            log.error("매칭 상태 확인 실패 - matchId: {}", matchId, e);
        }
    }

    /**
     * 매칭 완료 처리
     */
    private void completeMatch(String matchId) {
        try {
            // 매칭 상태를 완료로 변경
            matchChatSessionService.updateMatchStatus(matchId, "completed");

            // 모든 참여자에게 매칭 완료 알림
            Map<String, Object> completeMessage = createMatchCompleteMessage(matchId);
            broadcastToMatchParticipants(matchId, completeMessage);

            // 매칭 완료 이벤트 발행
            Map<String, Object> completeEvent = createMatchCompleteEvent(matchId);
            redisPubSubService.publishMessage(matchId, completeEvent);

            // 일정 시간 후 채팅방 자동 정리 스케줄링
            scheduleMatchRoomCleanup(matchId, 30); // 30초 후 정리

            log.info("매칭 완료 - matchId: {}", matchId);

        } catch (Exception e) {
            log.error("매칭 완료 처리 실패 - matchId: {}", matchId, e);
        }
    }

    /**
     * 매칭 대기 상태 업데이트
     */
    private void updateMatchWaitingStatus(String matchId, Long current, Integer required) {
        Map<String, Object> statusMessage = new HashMap<>();
        statusMessage.put("type", "match_status");
        statusMessage.put("status", "waiting");
        statusMessage.put("currentCount", current);
        statusMessage.put("requiredCount", required);
        statusMessage.put("message", String.format("매칭 대기 중... (%d/%d)", current, required));
        statusMessage.put("timestamp", System.currentTimeMillis());

        broadcastToMatchParticipants(matchId, statusMessage);
    }

    /**
     * 매칭 참여자들에게 브로드캐스트
     */
    private void broadcastToMatchParticipants(String matchId, Map<String, Object> message) {
        connectedUsers.entrySet().stream()
                .filter(entry -> {
                    UserSessionInfo userInfo = sessionToUser.get(entry.getKey());
                    return userInfo != null && matchId.equals(userInfo.getRoomId());
                })
                .forEach(entry -> {
                    try {
                        sendMessageToUser(entry.getValue(), message);
                    } catch (Exception e) {
                        log.error("매칭 참여자 메시지 전송 실패 - sessionId: {}", entry.getKey(), e);
                    }
                });
    }

    /**
     * 매칭 타임아웃 스케줄링
     */
    private void scheduleMatchTimeout(String matchId, UserSessionInfo userInfo) {
        Integer maxWaitTime = (Integer) userInfo.getAdditionalInfo().get("maxWaitTime");

        scheduler.schedule(() -> {
            try {
                String currentStatus = matchChatSessionService.getMatchStatus(matchId);
                if ("waiting".equals(currentStatus)) {
                    handleMatchTimeout(matchId);
                }
            } catch (Exception e) {
                log.error("매칭 타임아웃 처리 실패 - matchId: {}", matchId, e);
            }
        }, maxWaitTime, TimeUnit.SECONDS);
    }

    /**
     * 매칭 타임아웃 처리
     */
    private void handleMatchTimeout(String matchId) {
        try {
            matchChatSessionService.updateMatchStatus(matchId, "timeout");

            Map<String, Object> timeoutMessage = createMatchTimeoutMessage(matchId);
            broadcastToMatchParticipants(matchId, timeoutMessage);

            // 타임아웃 후 채팅방 정리
            scheduleMatchRoomCleanup(matchId, 10); // 10초 후 정리

            log.info("매칭 타임아웃 - matchId: {}", matchId);

        } catch (Exception e) {
            log.error("매칭 타임아웃 처리 실패 - matchId: {}", matchId, e);
        }
    }

    /**
     * 매칭 채팅방 정리 스케줄링
     */
    private void scheduleMatchRoomCleanup(String matchId, int delaySeconds) {
        scheduler.schedule(() -> {
            try {
                cleanupMatchRoom(matchId);
            } catch (Exception e) {
                log.error("매칭 채팅방 정리 실패 - matchId: {}", matchId, e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * 매칭 채팅방 정리
     */
    private void cleanupMatchRoom(String matchId) {
        try {
            // 해당 매칭의 모든 WebSocket 연결 종료
            connectedUsers.entrySet().removeIf(entry -> {
                UserSessionInfo userInfo = sessionToUser.get(entry.getKey());
                if (userInfo != null && matchId.equals(userInfo.getRoomId())) {
                    try {
                        entry.getValue().close();
                        sessionToUser.remove(entry.getKey());
                        return true;
                    } catch (Exception e) {
                        log.error("WebSocket 연결 종료 실패 - sessionId: {}", entry.getKey(), e);
                        return true;
                    }
                }
                return false;
            });

            // Redis에서 매칭 데이터 정리
            matchChatRoomService.cleanupMatchRoom(matchId);
            matchChatSessionService.cleanupMatchSession(matchId);

            // Redis Pub/Sub 구독 해제
            redisPubSubService.unsubscribeFromRoom(matchId);

            log.info("매칭 채팅방 정리 완료 - matchId: {}", matchId);

        } catch (Exception e) {
            log.error("매칭 채팅방 정리 실패 - matchId: {}", matchId, e);
        }
    }

    /**
     * 매칭 채팅방 정리 스케줄러 시작
     */
    private void startMatchCleanupScheduler() {
        // 주기적으로 오래된 매칭 채팅방 정리 (1분마다)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredMatchRooms();
            } catch (Exception e) {
                log.error("만료된 매칭 채팅방 정리 실패", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 만료된 매칭 채팅방들 정리
     */
    private void cleanupExpiredMatchRooms() {
        try {
            // 1시간 이상 지난 매칭 채팅방들 정리
            LocalDateTime expireTime = LocalDateTime.now().minusHours(1);
            matchChatRoomService.cleanupExpiredRooms(expireTime);

        } catch (Exception e) {
            log.error("만료된 매칭 채팅방 정리 실패", e);
        }
    }

    // ========== 메시지 생성 메서드들 ==========

    /**
     * 매칭 채팅 메시지 생성
     */
    private Map<String, Object> createMatchChatMessage(UserSessionInfo userInfo, String content) {
        Map<String, Object> message = createChatMessage(userInfo, content);

        // 매칭 채팅 특화 정보 추가
        message.put("chatType", "match");
        message.put("matchId", userInfo.getRoomId());
        message.put("serverId", getServerId());

        return message;
    }

    /**
     * 매칭 참여 이벤트 생성
     */
    private Map<String, Object> createMatchJoinEvent(UserSessionInfo userInfo) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "match_join");
        event.put("userId", userInfo.getUserId());
        event.put("userName", userInfo.getUserName());
        event.put("matchId", userInfo.getRoomId());
        event.put("timestamp", System.currentTimeMillis());
        event.put("serverId", getServerId());
        return event;
    }

    /**
     * 매칭 퇴장 이벤트 생성
     */
    private Map<String, Object> createMatchLeaveEvent(UserSessionInfo userInfo) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "match_leave");
        event.put("userId", userInfo.getUserId());
        event.put("userName", userInfo.getUserName());
        event.put("matchId", userInfo.getRoomId());
        event.put("timestamp", System.currentTimeMillis());
        event.put("serverId", getServerId());
        return event;
    }

    /**
     * 매칭 완료 메시지 생성
     */
    private Map<String, Object> createMatchCompleteMessage(String matchId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "match_complete");
        message.put("matchId", matchId);
        message.put("status", "completed");
        message.put("message", "🎉 매칭이 완료되었습니다! 게임을 시작할 수 있습니다.");
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }

    /**
     * 매칭 완료 이벤트 생성
     */
    private Map<String, Object> createMatchCompleteEvent(String matchId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "match_completed");
        event.put("matchId", matchId);
        event.put("timestamp", System.currentTimeMillis());
        event.put("serverId", getServerId());

        try {
            // 참여자 목록 추가
            event.put("participants", matchChatSessionService.getParticipants(matchId));
        } catch (Exception e) {
            log.warn("매칭 완료 이벤트 참여자 목록 조회 실패 - matchId: {}", matchId, e);
        }

        return event;
    }

    /**
     * 매칭 타임아웃 메시지 생성
     */
    private Map<String, Object> createMatchTimeoutMessage(String matchId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "match_timeout");
        message.put("matchId", matchId);
        message.put("status", "timeout");
        message.put("message", "⏰ 매칭 시간이 초과되었습니다. 다시 시도해주세요.");
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }

    // ========== 유틸리티 메서드들 ==========

    /**
     * 너무 빈번한 메시지인지 확인
     */
    private boolean isTooFrequentMessage(UserSessionInfo userInfo) {
        Long lastMessageTime = (Long) userInfo.getAdditionalInfo().get("lastMessageTime");
        long currentTime = System.currentTimeMillis();

        if (lastMessageTime != null) {
            // 매칭 채팅은 1초 간격 제한
            if (currentTime - lastMessageTime < 1000) {
                return true;
            }
        }

        userInfo.addAdditionalInfo("lastMessageTime", currentTime);
        return false;
    }

    /**
     * WebSocket 세션에서 매칭 ID 추출
     */
    private String extractMatchId(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        String matchId = (String) attributes.get("matchId");

        if (matchId == null) {
            // URL에서 추출 시도
            String uri = session.getUri().toString();
            matchId = parseMatchIdFromUri(uri);
        }

        if (matchId == null) {
            // 새로운 매칭 ID 생성
            matchId = generateNewMatchId();
        }

        return matchId;
    }

    /**
     * WebSocket 세션에서 사용자 ID 추출
     */
    private String extractUserId(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        String userId = (String) attributes.get("userId");

        if (userId == null) {
            // JWT 토큰에서 추출하거나 임시 ID 생성
            userId = "match_user_" + session.getId().substring(0, 8);
        }

        return userId;
    }

    /**
     * 사용자 이름 생성/추출
     */
    private String extractUserName(WebSocketSession session, String userId) {
        Map<String, Object> attributes = session.getAttributes();
        String userName = (String) attributes.get("userName");

        if (userName == null) {
            // 기본 사용자명 생성
            userName = "Player_" + userId.substring(userId.length() - 4);
        }

        return userName;
    }

    /**
     * URI에서 매칭 ID 파싱
     */
    private String parseMatchIdFromUri(String uri) {
        try {
            if (uri.contains("matchId=")) {
                String[] parts = uri.split("matchId=");
                if (parts.length > 1) {
                    return parts[1].split("&")[0];
                }
            }
        } catch (Exception e) {
            log.warn("URI에서 matchId 파싱 실패: {}", uri, e);
        }
        return null;
    }

    /**
     * 새로운 매칭 ID 생성
     */
    private String generateNewMatchId() {
        return "match_" + System.currentTimeMillis() + "_" +
                Integer.toHexString((int)(Math.random() * 65536));
    }

    /**
     * 최대 대기 시간 반환
     */
    private Integer getMaxWaitTime() {
        // 설정에서 가져오거나 기본값 사용 (5분)
        return 300; // 300초 = 5분
    }

    /**
     * 서버 식별자 반환
     */
    private String getServerId() {
        return System.getProperty("server.id", "match-server-1");
    }

    // ========== 공개 메서드들 (모니터링/관리용) ==========

    /**
     * 매칭 채팅 통계 정보 조회
     */
    public Map<String, Object> getMatchChatStats() {
        Map<String, Object> stats = getConnectionStats();

        try {
            // 매칭 채팅 특화 통계 추가
            Map<String, Object> matchStats = new HashMap<>();

            // 매칭 상태별 통계
            Map<String, Integer> statusCounts = new HashMap<>();
            sessionToUser.values().forEach(userInfo -> {
                String status = (String) userInfo.getAdditionalInfo().get("matchStatus");
                statusCounts.merge(status, 1, Integer::sum);
            });
            matchStats.put("statusCounts", statusCounts);

            // 매칭 ID별 참여자 수
            Map<String, Long> matchParticipants = new HashMap<>();
            sessionToUser.values().forEach(userInfo -> {
                String matchId = userInfo.getRoomId();
                matchParticipants.merge(matchId, 1L, Long::sum);
            });
            matchStats.put("matchParticipants", matchParticipants);

            // 전체 활성 매칭 수
            matchStats.put("activeMatches", matchParticipants.size());

            stats.put("matchStats", matchStats);

        } catch (Exception e) {
            log.error("매칭 채팅 통계 조회 실패", e);
        }

        return stats;
    }

    /**
     * 특정 매칭의 상세 정보 조회
     */
    public Map<String, Object> getMatchDetails(String matchId) {
        Map<String, Object> details = new HashMap<>();

        try {
            details.put("matchId", matchId);
            details.put("status", matchChatSessionService.getMatchStatus(matchId));
            details.put("participantCount", matchChatSessionService.getParticipantCount(matchId));
            details.put("requiredCount", matchChatRoomService.getRequiredParticipants(matchId));
            details.put("maxParticipants", matchChatRoomService.getMaxParticipants(matchId));
            details.put("participants", matchChatSessionService.getParticipants(matchId));

            // 현재 서버에 연결된 참여자들
            long connectedCount = sessionToUser.values().stream()
                    .filter(userInfo -> matchId.equals(userInfo.getRoomId()))
                    .count();
            details.put("connectedInThisServer", connectedCount);

        } catch (Exception e) {
            log.error("매칭 상세 정보 조회 실패 - matchId: {}", matchId, e);
        }

        return details;
    }

    /**
     * 매칭 강제 완료 (관리자용)
     */
    public boolean forceCompleteMatch(String matchId) {
        try {
            log.info("매칭 강제 완료 요청 - matchId: {}", matchId);
            completeMatch(matchId);
            return true;
        } catch (Exception e) {
            log.error("매칭 강제 완료 실패 - matchId: {}", matchId, e);
            return false;
        }
    }

    /**
     * 매칭 강제 취소 (관리자용)
     */
    public boolean forceCancelMatch(String matchId) {
        try {
            log.info("매칭 강제 취소 요청 - matchId: {}", matchId);

            // 취소 메시지 전송
            Map<String, Object> cancelMessage = new HashMap<>();
            cancelMessage.put("type", "match_cancelled");
            cancelMessage.put("message", "관리자에 의해 매칭이 취소되었습니다.");
            cancelMessage.put("timestamp", System.currentTimeMillis());

            broadcastToMatchParticipants(matchId, cancelMessage);

            // 매칭 정리
            cleanupMatchRoom(matchId);

            return true;
        } catch (Exception e) {
            log.error("매칭 강제 취소 실패 - matchId: {}", matchId, e);
            return false;
        }
    }

    /**
     * 스케줄러 종료 (애플리케이션 종료 시)
     */
    public void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info("매칭 채팅 스케줄러 종료 완료");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("매칭 채팅 스케줄러 강제 종료");
        }
    }
}