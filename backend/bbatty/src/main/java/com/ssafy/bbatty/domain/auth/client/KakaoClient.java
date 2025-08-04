package com.ssafy.bbatty.domain.auth.client;

import com.ssafy.bbatty.domain.auth.dto.response.KakaoUserResponse;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 카카오 API 클라이언트
 * WebClient를 사용한 비동기 HTTP 통신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoClient {

    private final WebClient webClient;

    @Value("${kakao.user-info-url}")
    private String userInfoUrl;

    @Value("${kakao.timeout-seconds:10}")
    private int timeoutSeconds;

    /**
     * 카카오 사용자 정보 조회
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 사용자 정보
     */
    public KakaoUserResponse getUserInfoFromKakao(String accessToken) {
        try {
            log.debug("카카오 사용자 정보 조회 시작");

            return webClient.get()
                    .uri(userInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("카카오 API 클라이언트 오류: {}", response.statusCode());
                        return Mono.error(new ApiException(ErrorCode.KAKAO_AUTH_FAILED));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("카카오 API 서버 오류: {}", response.statusCode());
                        return Mono.error(new ApiException(ErrorCode.KAKAO_AUTH_FAILED));
                    })
                    .bodyToMono(KakaoUserResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnSuccess(response -> log.debug("카카오 사용자 정보 조회 성공: kakaoId={}", response.getKakaoId()))
                    .doOnError(error -> log.error("카카오 사용자 정보 조회 실패: {}", error.getMessage()))
                    .block();

        } catch (Exception e) {
            log.error("카카오 API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    /**
     * 카카오 액세스 토큰 유효성 검증
     * @param accessToken 카카오 액세스 토큰
     * @return 토큰 유효 여부
     */
    public boolean validateToken(String accessToken) {
        try {
            getUserInfoFromKakao(accessToken);
            return true;
        } catch (ApiException e) {
            log.warn("카카오 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}