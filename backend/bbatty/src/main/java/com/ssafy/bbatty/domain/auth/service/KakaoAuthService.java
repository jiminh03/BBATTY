package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.domain.auth.constants.AuthConstants;
import com.ssafy.bbatty.domain.auth.dto.external.KakaoUserInfoDto;
import com.ssafy.bbatty.domain.auth.exception.AuthExceptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class KakaoAuthService {

    private final WebClient webClient;
    private final String kakaoUserInfoUrl;
    private final int timeoutSeconds;

    public KakaoAuthService(
            @Value("${kakao.user-info-url}") String kakaoUserInfoUrl,
            @Value("${kakao.timeout-seconds:10}") int timeoutSeconds) {
        this.webClient = WebClient.builder().build();
        this.kakaoUserInfoUrl = kakaoUserInfoUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    public KakaoUserInfoDto getUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(kakaoUserInfoUrl)
                            .queryParam("property_keys", "[\"kakao_account.email\",\"kakao_account.name\",\"kakao_account.birthyear\",\"kakao_account.birthday\",\"kakao_account.gender\"]")
                            .build())
                    .header("Authorization", AuthConstants.KAKAO_TOKEN_PREFIX + accessToken)
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .retrieve()
                    .bodyToMono(KakaoUserInfoDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new AuthExceptions.KakaoAuthException();
        }
    }
}