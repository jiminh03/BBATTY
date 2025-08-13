package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.response.NewsSummaryDto;
import com.ssafy.bbatty.global.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<NewsSummaryDto> getNewsSummaryByTeam(Long teamId) {
        try {
            String redisKey = RedisKey.NEWS_SUMMARY + teamId;
            
            // Redis Hash에서 모든 뉴스 데이터 조회
            Map<Object, Object> hashEntries = redisTemplate.opsForHash().entries(redisKey);
            
            if (hashEntries == null || hashEntries.isEmpty()) {
                log.warn("팀 ID {} 뉴스 요약 데이터가 Redis에 없습니다.", teamId);
                return new ArrayList<>();
            }
            
            // Hash 데이터를 NewsSummaryDto로 변환
            List<NewsSummaryDto> summaries = new ArrayList<>();
            Map<String, NewsSummaryDto> newsMap = new HashMap<>();
            
            // Hash의 각 엔트리를 처리
            for (Map.Entry<Object, Object> entry : hashEntries.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                
                // key 형식: "news_0:title" 또는 "news_0:summary"
                String[] keyParts = key.split(":");
                if (keyParts.length == 2) {
                    String newsIndex = keyParts[0];
                    String fieldType = keyParts[1];
                    
                    NewsSummaryDto dto = newsMap.computeIfAbsent(newsIndex, k -> new NewsSummaryDto());
                    
                    if ("title".equals(fieldType)) {
                        dto.setTitle(value);
                    } else if ("summary".equals(fieldType)) {
                        dto.setSummary(value);
                    }
                }
            }
            
            // Map에서 List로 변환
            summaries.addAll(newsMap.values());
            
            log.info("팀 ID {} 뉴스 요약 {} 개 조회 완료", teamId, summaries.size());
            return summaries;
            
        } catch (Exception e) {
            log.error("팀 ID {} 뉴스 요약 조회 중 오류 발생: {}", teamId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}