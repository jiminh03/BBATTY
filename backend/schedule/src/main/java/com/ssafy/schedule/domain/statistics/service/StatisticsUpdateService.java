package com.ssafy.schedule.domain.statistics.service;

/**
 * 통계 업데이트 서비스 인터페이스
 * - 경기 결과 업데이트 후 관련 사용자 통계 재계산
 */
public interface StatisticsUpdateService {
    
    /**
     * 특정 날짜의 직관 인증자들의 통계 재계산
     * 
     * @param date 날짜 (yyyy-MM-dd 형식)
     */
    void updateUserStatsForDate(String date);
}