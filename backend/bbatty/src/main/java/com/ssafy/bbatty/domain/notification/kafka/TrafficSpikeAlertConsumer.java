package com.ssafy.bbatty.domain.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.notification.entity.NotificationSetting;
import com.ssafy.bbatty.domain.notification.repository.NotificationSettingRepository;
import com.ssafy.bbatty.domain.notification.service.PushNotificationService;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.global.constants.RedisKey;
import com.ssafy.bbatty.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficSpikeAlertConsumer {

    private final NotificationSettingRepository notificationSettingRepository;
    private final TeamRepository teamRepository;
    private final PushNotificationService pushNotificationService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "traffic-spike-alert", groupId = "bbatty-notification-group")
    public void handleTrafficSpikeAlert(String message) {
        try {
            Map<String, Object> alertData = objectMapper.readValue(message, Map.class);
            
            String alertType = (String) alertData.get("alertType");
            if (!"TRAFFIC_SPIKE".equals(alertType)) {
                log.debug("트래픽 급증 알림이 아닌 메시지 무시 - alertType: {}", alertType);
                return;
            }

            Long teamId = Long.valueOf(alertData.get("teamId").toString());
            String teamName = getTeamName(teamId);
            
            log.info("트래픽 급증 알림 처리 시작 - teamId: {}, teamName: {}", teamId, teamName);

            List<NotificationSetting> candidateUsers = notificationSettingRepository
                    .findTrafficSpikeAlertEnabledUsers(teamId);

            if (candidateUsers.isEmpty()) {
                log.info("트래픽 급증 알림 대상 사용자 없음 - teamId: {}", teamId);
                return;
            }

            List<NotificationSetting> unAuthenticatedUsers = filterUnAuthenticatedUsers(candidateUsers);

            if (unAuthenticatedUsers.isEmpty()) {
                log.info("미인증 사용자 없음 (모두 직관 인증 완료) - teamId: {}", teamId);
                return;
            }

            log.info("트래픽 급증 알림 발송 대상 - teamId: {}, total: {}, unAuthenticated: {}", 
                    teamId, candidateUsers.size(), unAuthenticatedUsers.size());

            pushNotificationService.sendBatchNotifications(unAuthenticatedUsers, teamName);

        } catch (Exception e) {
            log.error("트래픽 급증 알림 처리 중 오류 발생", e);
        }
    }

    private String getTeamName(Long teamId) {
        return teamRepository.findById(teamId)
                .map(team -> team.getName())
                .orElse("Unknown Team");
    }

    private List<NotificationSetting> filterUnAuthenticatedUsers(List<NotificationSetting> candidateUsers) {
        LocalDate today = LocalDate.now();
        
        return candidateUsers.stream()
                .filter(setting -> !isUserAuthenticated(setting.getUser().getId(), today))
                .toList();
    }

    private boolean isUserAuthenticated(Long userId, LocalDate date) {
        try {
            String attendancePattern = RedisKey.USER_ATTENDANCE_GAME + userId + ":*";
            
            return redisUtil.getKeys(attendancePattern).stream()
                    .anyMatch(key -> {
                        String value = redisUtil.getValue(key);
                        return "ATTENDED".equals(value);
                    });
                    
        } catch (Exception e) {
            log.warn("사용자 직관 인증 상태 확인 실패 - userId: {}", userId, e);
            return false;
        }
    }
}