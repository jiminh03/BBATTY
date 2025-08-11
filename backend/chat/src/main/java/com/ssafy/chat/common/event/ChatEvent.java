package com.ssafy.chat.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 채팅 이벤트 기본 클래스
 * 모든 채팅 관련 이벤트의 공통 속성을 정의
 */
@Getter
public abstract class ChatEvent extends ApplicationEvent {
    
    private final String eventId;
    private final LocalDateTime eventTime;
    private final String eventType;
    
    protected ChatEvent(Object source, String eventType) {
        super(source);
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventTime = LocalDateTime.now();
        this.eventType = eventType;
    }
    
    protected ChatEvent(Object source, String eventId, String eventType) {
        super(source);
        this.eventId = eventId;
        this.eventTime = LocalDateTime.now();
        this.eventType = eventType;
    }
}