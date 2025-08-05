package com.ssafy.bbatty.global.constants;

import com.ssafy.bbatty.global.exception.ApiException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * KBO 경기장 위치 정보 (정규구장 + 제2구장)
 * 
 * ⚠️ 크롤링 팀과 공유되는 enum
 */
@Getter
@RequiredArgsConstructor
public enum Stadium {
    
    // ========================================
    // KBO 정규 9개 구장 (구글맵 정확한 좌표)
    // ========================================
    
    // 서울 지역
    JAMSIL("잠실야구장", new BigDecimal("37.5121528"), new BigDecimal("127.0717917")),
    GOCHEOK("고척스카이돔", new BigDecimal("37.4981583"), new BigDecimal("126.8668306")),
    
    // 경기 지역  
    SUWON("수원KT위즈파크", new BigDecimal("37.2997194"), new BigDecimal("127.0097028")),
    
    // 인천 지역
    INCHEON("인천SSG랜더스필드", new BigDecimal("37.4369444"), new BigDecimal("126.6934583")),
    
    // 대전 지역
    DAEJEON("대전한화생명볼파크", new BigDecimal("36.3171306"), new BigDecimal("127.4287167")),
    
    // 광주 지역
    GWANGJU("광주기아챔피언스필드", new BigDecimal("35.1681083"), new BigDecimal("126.8890194")),
    
    // 대구 지역
    DAEGU("대구삼성라이온즈파크", new BigDecimal("35.8410417"), new BigDecimal("128.6818889")),
    
    // 부산 지역
    BUSAN("부산사직야구장", new BigDecimal("35.1940833"), new BigDecimal("129.0617472")),
    
    // 창원 지역
    CHANGWON("창원NC파크", new BigDecimal("35.2225694"), new BigDecimal("128.5825639")),
    
    // ========================================
    // KBO 제2구장
    // ========================================
    
    // 충북 청주 (청주종합운동장 야구장) - 구글맵 기준
    CHEONGJU("청주야구장", new BigDecimal("36.6279853"), new BigDecimal("127.4881449")),
    
    // 경북 포항 (포항스틸러스파크) - 구글맵 기준
    POHANG("포항야구장", new BigDecimal("36.0158639"), new BigDecimal("129.3433194")),
    
    // 울산 문수 (문수야구장) - 구글맵 기준
    ULSAN("울산문수야구장", new BigDecimal("35.5396728"), new BigDecimal("129.2634017"));
    
    private final String stadiumName;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    
    // 경기장명을 키로 하는 Map (빠른 조회를 위한 데이터 구조 선택)
    private static final Map<String, Stadium> STADIUM_MAP = 
        Arrays.stream(values())
              .collect(Collectors.toMap(
                  Stadium::getStadiumName, 
                  Function.identity()
              ));
    
    /**
     * 경기장명으로 Stadium 조회 (구성해놓은 Map 사용)
     */
    public static Stadium findByName(String stadiumName) {
        Stadium stadium = STADIUM_MAP.get(stadiumName);
        if (stadium == null) {
            throw new ApiException(ErrorCode.STADIUM_INFO_NOT_FOUND);
        }
        return stadium;
    }
}