package com.ssafy.bbatty.domain.attendance.dto.redis;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Redis에 캐시할 활성 경기 정보
 */
@Getter
@Builder
public class ActiveGame implements Serializable {
    
    private Long gameId;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime gameDateTime;
    private String stadium;
    private String status;
    
    // 구장 위치 정보 (함께 캐싱)
    private BigDecimal stadiumLatitude;
    private BigDecimal stadiumLongitude;
    private String stadiumDisplayName;
}
