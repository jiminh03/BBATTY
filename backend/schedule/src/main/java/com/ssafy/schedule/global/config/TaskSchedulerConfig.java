package com.ssafy.schedule.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 동적 스케줄링을 위한 TaskScheduler 설정
 */
@Configuration
public class TaskSchedulerConfig {

    /**
     * 경기 이벤트 스케줄링용 TaskScheduler Bean
     * - 경기 시작 2시간 전 이벤트 스케줄링에 사용
     * - 동적으로 스케줄 추가/제거 가능
     */
    @Bean("gameEventTaskScheduler")
    public TaskScheduler gameEventTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // 동시 실행 가능한 스케줄 수
        scheduler.setThreadNamePrefix("game-event-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}