package com.ssafy.chat.match.kafka;

import com.ssafy.chat.match.service.GameInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameInfoKafkaConsumer {

    private final GameInfoService gameInfoService;

    @KafkaListener(topics = "game-info", groupId = "match-chat-service")
    public void consumeGameInfo(String message) {
        try {
            log.info("Received game info message from Kafka");
            gameInfoService.processGameInfoMessage(message);
            log.info("Successfully processed game info message");
        } catch (Exception e) {
            log.error("Failed to process game info message", e);
        }
    }
}