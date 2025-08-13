package com.ssafy.schedule.domain.chat.scheduler;

import com.ssafy.schedule.domain.chat.kafka.ChatCleanupKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 채팅방 정리 스케줄러
 * 매일 23:50, 23:54, 23:55에 각각 다른 지연시간으로 삭제 이벤트 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatCleanupScheduler {
    
    private final ChatCleanupKafkaProducer chatCleanupKafkaProducer;
    
    /**
     * 첫 번째 경고: 매일 23시 50분
     * 5분 후 종료 안내
     */
    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    public void sendFirstWarning() {
        log.info("🚨 첫 번째 경고 스케줄 실행 - 5분 후 채팅방 종료 안내");
        try {
            chatCleanupKafkaProducer.sendWarning1Event();
            log.info("✅ 첫 번째 경고 이벤트 전송 완료");
        } catch (Exception e) {
            log.error("❌ 첫 번째 경고 이벤트 전송 실패", e);
        }
    }
    
    /**
     * 두 번째 경고: 매일 23시 54분  
     * 1분 후 종료 안내
     */
    @Scheduled(cron = "0 54 23 * * *", zone = "Asia/Seoul")
    public void sendSecondWarning() {
        log.info("⚠️ 두 번째 경고 스케줄 실행 - 1분 후 채팅방 종료 안내");
        try {
            chatCleanupKafkaProducer.sendWarning2Event();
            log.info("✅ 두 번째 경고 이벤트 전송 완료");
        } catch (Exception e) {
            log.error("❌ 두 번째 경고 이벤트 전송 실패", e);
        }
    }
    
    /**
     * 실제 정리: 매일 23시 55분
     * 지금 바로 삭제
     */
    @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Seoul")
    public void executeCleanup() {
        log.info("🧹 채팅방 정리 스케줄 실행 - 즉시 삭제");
        try {
            chatCleanupKafkaProducer.sendCleanupEvent();
            log.info("✅ 채팅방 정리 이벤트 전송 완료");
        } catch (Exception e) {
            log.error("❌ 채팅방 정리 이벤트 전송 실패", e);
        }
    }
}