package com.ssafy.schedule.domain.ai.service;

import com.ssafy.schedule.domain.ai.dto.NaverNewsResponse;
import com.ssafy.schedule.global.util.DateFilterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.naver-api.baseurl}")
    private String baseUrl;

    @Value("${spring.ai.naver-api.client-id}")
    private String clientId;

    @Value("${spring.ai.naver-api.client-secret}")
    private String clientSecret;

    public NaverNewsResponse searchNews (String query, int display, int start, String sort) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("query", query)
                .queryParam("display", display)
                .queryParam("start", start)
                .queryParam("sort", sort)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 요청 보내기
        ResponseEntity<NaverNewsResponse> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, NaverNewsResponse.class);

        return response.getBody();
    }

}
