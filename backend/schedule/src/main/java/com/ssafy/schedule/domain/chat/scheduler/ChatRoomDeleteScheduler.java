package com.ssafy.schedule.domain.chat.scheduler;

import com.ssafy.schedule.domain.chat.dto.ChatRoomDeleteEventDto;
import com.ssafy.schedule.domain.chat.kafka.ChatEventKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 채팅방 삭제 이벤트 스케줄러
 * 매일 23시 50분에 당일 채팅방 삭제 이벤트 전송
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomDeleteScheduler {
    
    private final ChatEventKafkaProducer chatEventKafkaProducer;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 매일 23시 50분에 실행
     * 당일 날짜(서울 기준)를 채팅방 삭제 이벤트로 전송
     */
    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    public void sendDailyChatRoomDeleteEvent() {
        try {
            // 서울 시간 기준 오늘 날짜 계산
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            String dateString = today.format(DATE_FORMATTER);
            
            log.info("🗑️ 채팅방 삭제 이벤트 전송 시작: {}", dateString);
            
            // 채팅방 삭제 이벤트 DTO 생성
            ChatRoomDeleteEventDto eventDto = ChatRoomDeleteEventDto.of(dateString);
            
            // Kafka로 이벤트 전송
            chatEventKafkaProducer.sendChatRoomDeleteEvent(eventDto);
            
            log.info("✅ 채팅방 삭제 이벤트 전송 완료: {}", dateString);
            
        } catch (Exception e) {
            log.error("❌ 채팅방 삭제 이벤트 전송 실패", e);
        }
    }
}