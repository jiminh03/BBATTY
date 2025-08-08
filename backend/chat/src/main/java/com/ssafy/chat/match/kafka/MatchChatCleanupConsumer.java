package com.ssafy.chat.match.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.global.constants.ChatRedisKey;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.service.MatchChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Scheduler에서 전송하는 매칭 채팅방 정리 명령을 처리하는 Kafka Consumer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatCleanupConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final MatchChatService matchChatService;
    private final ObjectMapper objectMapper;
    private final KafkaAdmin kafkaAdmin;

    @KafkaListener(topics = "match-chat-cleanup", groupId = "chat-cleanup-consumer-group")
    public void handleCleanupRequest(String message) {
        try {
            log.info("매칭 채팅방 정리 요청 수신: {}", message);
            
            // 1. 메시지 파싱
            Map<String, Object> cleanupRequest = parseCleanupRequest(message);
            String action = (String) cleanupRequest.get("action");
            String targetDate = (String) cleanupRequest.get("date");
            
            // 2. 요청 유효성 검증
            validateCleanupRequest(action, targetDate);
            
            // 3. 액션별 처리
            switch (action) {
                case "warning_1" -> handleWarning1(targetDate);
                case "warning_2" -> handleWarning2(targetDate);
                case "cleanup" -> handleCleanup(targetDate);
                default -> {
                    log.error("알 수 없는 액션: {}", action);
                    throw new ApiException(ErrorCode.CLEANUP_ACTION_UNKNOWN);
                }
            }
            
        } catch (ApiException e) {
            log.error("정리 요청 처리 실패 - ErrorCode: {}, Message: {}",
                    e.getErrorCode(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
        }
    }

    /**
     * 정리 요청 메시지 파싱
     */
    private Map<String, Object> parseCleanupRequest(String message) {
        try {
            return objectMapper.readValue(message, Map.class);
        } catch (Exception e) {
            log.error("정리 요청 메시지 파싱 실패: {}", message, e);
            throw new ApiException(ErrorCode.CLEANUP_REQUEST_INVALID);
        }
    }

    /**
     * 정리 요청 유효성 검증
     */
    private void validateCleanupRequest(String action, String targetDate) {
        if (targetDate == null || targetDate.trim().isEmpty()) {
            log.error("정리 대상 날짜 누락 - action: {}, date: {}", action, targetDate);
            throw new ApiException(ErrorCode.CLEANUP_DATE_MISSING);
        }
        
        if (action == null || action.trim().isEmpty()) {
            log.error("정리 액션 누락 - action: {}, date: {}", action, targetDate);
            throw new ApiException(ErrorCode.CLEANUP_ACTION_UNKNOWN);
        }
    }

    /**
     * 첫 번째 경고 (23:54) - 6분 후 종료 안내
     */
    private void handleWarning1(String targetDate) {
        log.info("첫 번째 경고 처리 시작 - 날짜: {}", targetDate);
        
        try {
            Set<String> matchIds = getMatchIdsByDate(targetDate);
            if (matchIds.isEmpty()) {
                log.info("📭 경고할 채팅방이 없음 - 날짜: {}", targetDate);
                return;
            }
            
            String warningMessage = "5분 후 채팅방이 종료됩니다. 메시지는 저장되지 않습니다.";
            
            int successCount = 0;
            int failCount = 0;
            
            for (String matchId : matchIds) {
                try {
                    matchChatService.sendSystemMessageToRoom(matchId, warningMessage);
                    successCount++;
                } catch (Exception e) {
                    log.error("첫 번째 경고 전송 실패 - matchId: {}", matchId, e);
                    failCount++;
                }
            }
            
            log.info("첫 번째 경고 완료 - 날짜: {}, 성공: {}, 실패: {}",
                    targetDate, successCount, failCount);
            
            if (failCount > 0) {
                log.warn("일부 경고 전송 실패 - 실패 수: {}", failCount);
            }
            
        } catch (Exception e) {
            log.error("첫 번째 경고 처리 중 오류 - 날짜: {}", targetDate, e);
            throw new ApiException(ErrorCode.CLEANUP_SYSTEM_MESSAGE_FAILED);
        }
    }

    /**
     * 두 번째 경고 (23:58) - 2분 후 종료 안내
     */
    private void handleWarning2(String targetDate) {
        log.info("⚠두 번째 경고 처리 시작 - 날짜: {}", targetDate);
        
        try {
            Set<String> matchIds = getMatchIdsByDate(targetDate);
            if (matchIds.isEmpty()) {
                log.info("📭 경고할 채팅방이 없음 - 날짜: {}", targetDate);
                return;
            }
            
            String warningMessage = "1분 후 채팅방이 종료됩니다.";
            
            int successCount = 0;
            int failCount = 0;
            
            for (String matchId : matchIds) {
                try {
                    matchChatService.sendSystemMessageToRoom(matchId, warningMessage);
                    successCount++;
                } catch (Exception e) {
                    log.error("두 번째 경고 전송 실패 - matchId: {}", matchId, e);
                    failCount++;
                }
            }
            
            log.info("두 번째 경고 완료 - 날짜: {}, 성공: {}, 실패: {}",
                    targetDate, successCount, failCount);
            
            if (failCount > 0) {
                log.warn("⚠일부 경고 전송 실패 - 실패 수: {}", failCount);
            }
            
        } catch (Exception e) {
            log.error("두 번째 경고 처리 중 오류 - 날짜: {}", targetDate, e);
            throw new ApiException(ErrorCode.CLEANUP_SYSTEM_MESSAGE_FAILED);
        }
    }

    /**
     * 실제 정리 (00:00) - 세션 종료 및 리소스 정리
     */
    private void handleCleanup(String targetDate) {
        log.info("🧹 매칭 채팅방 정리 시작 - 날짜: {}", targetDate);
        
        try {
            Set<String> matchIds = getMatchIdsByDate(targetDate);
            if (matchIds.isEmpty()) {
                log.info("정리할 채팅방이 없음 - 날짜: {}", targetDate);
                return;
            }
            
            log.info("정리 대상 채팅방 수: {} - 날짜: {}", matchIds.size(), targetDate);
            
            int cleanedCount = 0;
            int failedCount = 0;
            
            // 1. 각 채팅방 정리
            for (String matchId : matchIds) {
                try {
                    cleanupSingleMatchChatRoom(matchId);
                    cleanedCount++;
                } catch (ApiException e) {
                    log.error("개별 채팅방 정리 실패 - matchId: {}, ErrorCode: {}", 
                            matchId, e.getErrorCode(), e);
                    failedCount++;
                } catch (Exception e) {
                    log.error("개별 채팅방 정리 중 예상치 못한 오류 - matchId: {}", matchId, e);
                    failedCount++;
                }
            }
            
            // 2. 날짜별 목록 키 삭제
            cleanupDateListKey(targetDate);
            
            log.info("매칭 채팅방 정리 완료 - 날짜: {}, 성공: {}, 실패: {}",
                    targetDate, cleanedCount, failedCount);
            
        } catch (Exception e) {
            log.error("매칭 채팅방 정리 중 오류 - 날짜: {}", targetDate, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }

    /**
     * 특정 날짜의 매칭 채팅방 ID 목록 조회
     */
    private Set<String> getMatchIdsByDate(String targetDate) {
        try {
            String dateListKey = ChatRedisKey.getMatchRoomListByDateKey(targetDate);
            Set<String> matchIds = redisTemplate.opsForSet().members(dateListKey);
            return matchIds != null ? matchIds : Set.of();
        } catch (Exception e) {
            log.error("날짜별 채팅방 목록 조회 실패 - date: {}", targetDate, e);
            throw new ApiException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 개별 매칭 채팅방 정리
     */
    private void cleanupSingleMatchChatRoom(String matchId) {
        log.debug("개별 채팅방 정리 시작 - matchId: {}", matchId);
        
        try {
            // 1. 종료 메시지 전송
            sendShutdownMessage(matchId);
            
            // 2. WebSocket 세션 강제 종료
            closeWebSocketSessions(matchId);
            
            // 3. Kafka 토픽 삭제
            deleteKafkaTopic(matchId);
            
            // 4. Redis 키 정리
            cleanupRedisKeys(matchId);
            
            log.debug("개별 채팅방 정리 완료 - matchId: {}", matchId);
            
        } catch (ApiException e) {
            log.error("개별 채팅방 정리 실패 - matchId: {}, ErrorCode: {}", 
                    matchId, e.getErrorCode(), e);
            throw e;
        } catch (Exception e) {
            log.error("개별 채팅방 정리 중 예상치 못한 오류 - matchId: {}", matchId, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }

    /**
     * 종료 메시지 전송
     */
    private void sendShutdownMessage(String matchId) {
        try {
            matchChatService.sendSystemMessageToRoom(matchId, "채팅방이 종료되었습니다.");
            Thread.sleep(1000); // 1초 대기 (메시지 전송 완료 보장)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("종료 메시지 전송 대기 중 인터럽트 - matchId: {}", matchId);
        } catch (Exception e) {
            log.error("종료 메시지 전송 실패 - matchId: {}", matchId, e);
            throw new ApiException(ErrorCode.CLEANUP_SYSTEM_MESSAGE_FAILED);
        }
    }

    /**
     * WebSocket 세션 강제 종료
     */
    private void closeWebSocketSessions(String matchId) {
        try {
            matchChatService.forceCloseRoomSessions(matchId);
        } catch (Exception e) {
            log.error("WebSocket 세션 강제 종료 실패 - matchId: {}", matchId, e);
            throw new ApiException(ErrorCode.CLEANUP_SESSION_FAILED);
        }
    }

    /**
     * Kafka 토픽 삭제
     */
    private void deleteKafkaTopic(String matchId) {
        String topicName = "match-chat-" + matchId;
        
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DeleteTopicsResult deleteResult = adminClient.deleteTopics(Set.of(topicName));
            deleteResult.all().get(); // 완료 대기
            
            log.debug("🗑️ Kafka 토픽 삭제 완료 - topic: {}", topicName);
            
        } catch (Exception e) {
            log.error("Kafka 토픽 삭제 실패 - matchId: {}, topic: {}", matchId, topicName, e);
            // Kafka 토픽 삭제 실패는 치명적이지 않으므로 경고만 출력하고 계속 진행
            log.warn("⚠Kafka 토픽 삭제 실패 (무시하고 계속 진행) - matchId: {}", matchId);
        }
    }

    /**
     * Redis 키 정리
     */
    private void cleanupRedisKeys(String matchId) {
        try {
            // 개별 채팅방 정보 삭제
            String roomInfoKey = ChatRedisKey.getMatchRoomInfoKey(matchId);
            Boolean roomDeleted = redisTemplate.delete(roomInfoKey);
            
            // 전체 목록에서 제거
            Long removedFromList = redisTemplate.opsForZSet().remove(ChatRedisKey.MATCH_ROOM_LIST, matchId);
            
            log.debug("🗑Redis 키 삭제 완료 - matchId: {}, roomDeleted: {}, removedFromList: {}",
                    matchId, roomDeleted, removedFromList);
            
        } catch (Exception e) {
            log.error("Redis 키 삭제 실패 - matchId: {}", matchId, e);
            throw new ApiException(ErrorCode.CLEANUP_REDIS_FAILED);
        }
    }

    /**
     * 날짜별 목록 키 삭제
     */
    private void cleanupDateListKey(String targetDate) {
        try {
            String dateListKey = ChatRedisKey.getMatchRoomListByDateKey(targetDate);
            Boolean deleted = redisTemplate.delete(dateListKey);
            
            log.info("🗑️ 날짜별 목록 키 삭제 완료 - key: {}, deleted: {}", dateListKey, deleted);
            
        } catch (Exception e) {
            log.error("날짜별 목록 키 삭제 실패 - date: {}", targetDate, e);
            throw new ApiException(ErrorCode.CLEANUP_REDIS_FAILED);
        }
    }
}