package com.ssafy.bbatty.domain.chat.game.service;

public interface GameChatTrafficService {

    /**
     * 팀별 트래픽 카운트 증가
     * @param teamId
     */
    void incrementTraffic(String teamId);

    /**
     * 특정 팀의 누적 트래픽 조회
     * @param teamId
     */
    Long getCurrentTraffic(String teamId);

    /**
     * 지정된 시각(yyyyMMddHH)의 시간별 트래픽 조회
     * @param teamId
     * @param hour
     * @return 시간별 트래픽 조회
     */
    Long getHourlyTraffic(String teamId, String hour);

    /**
     * 특정 팀 트래픽 카운트 리셋
     * @param teamId
     */
    void resetTraffic(String teamId);

    /**
     * 트래픽 급증(spike) 여부 감지
     * @param teamId 해당 팀 ID
     * @param threshold 임계값
     */
    boolean isTrafficSpike(String teamId, long threshold);
}
