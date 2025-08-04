package com.ssafy.bbatty.domain.auth.client;

import com.ssafy.bbatty.domain.auth.dto.response.KakaoUserResponse;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoClient 단위 테스트")
class KakaoClientTest {

    @Mock private WebClient webClient;
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    @InjectMocks private KakaoClient kakaoClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kakaoClient, "userInfoUrl", "https://kapi.kakao.com/v2/user/me");
        ReflectionTestUtils.setField(kakaoClient, "timeoutSeconds", 10);
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 - 성공")
    void getUserInfo_FromKakao_Success() {
        // Given
        String accessToken = "valid-kakao-token";
        KakaoUserResponse expectedResponse = createMockKakaoUserResponse();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq(HttpHeaders.AUTHORIZATION), eq("Bearer " + accessToken)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq(HttpHeaders.CONTENT_TYPE), anyString()))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserResponse.class))
                .thenReturn(Mono.just(expectedResponse));

        // When
        KakaoUserResponse result = kakaoClient.getUserInfoFromKakao(accessToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKakaoId()).isEqualTo(expectedResponse.getKakaoId());
        assertThat(result.getEmail()).isEqualTo(expectedResponse.getEmail());
        assertThat(result.getBirthYear()).isEqualTo(expectedResponse.getBirthYear());
        assertThat(result.getGender()).isEqualTo(expectedResponse.getGender());

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("https://kapi.kakao.com/v2/user/me");
        verify(requestHeadersSpec).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 - 4xx 클라이언트 오류")
    void getUserInfo_FromKakao_ClientError_ThrowsException() {
        // Given
        String accessToken = "invalid-token";
        WebClientResponseException clientError = WebClientResponseException.create(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserResponse.class))
                .thenReturn(Mono.error(clientError));

        // When & Then
        assertThatThrownBy(() -> kakaoClient.getUserInfoFromKakao(accessToken))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KAKAO_AUTH_FAILED);

        verify(webClient).get();
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 - 5xx 서버 오류")
    void getUserInfo_FromKakao_ServerError_ThrowsException() {
        // Given
        String accessToken = "valid-token";
        WebClientResponseException serverError = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserResponse.class))
                .thenReturn(Mono.error(serverError));

        // When & Then
        assertThatThrownBy(() -> kakaoClient.getUserInfoFromKakao(accessToken))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KAKAO_AUTH_FAILED);

        verify(webClient).get();
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 - 타임아웃 예외")
    void getUserInfo_FromKakao_Timeout_ThrowsException() {
        // Given
        String accessToken = "valid-token";
        RuntimeException timeoutException = new RuntimeException("Timeout");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserResponse.class))
                .thenReturn(Mono.error(timeoutException));

        // When & Then
        assertThatThrownBy(() -> kakaoClient.getUserInfoFromKakao(accessToken))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KAKAO_AUTH_FAILED);

        verify(webClient).get();
    }

    @Test
    @DisplayName("카카오 토큰 검증 - 유효한 토큰")
    void validateToken_ValidToken_ReturnsTrue() {
        // Given
        String accessToken = "valid-token";
        KakaoUserResponse response = createMockKakaoUserResponse();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserResponse.class))
                .thenReturn(Mono.just(response));

        // When
        boolean result = kakaoClient.validateToken(accessToken);

        // Then
        assertThat(result).isTrue();
        verify(webClient).get();
    }

    @Test
    @DisplayName("카카오 토큰 검증 - 무효한 토큰")
    void validateToken_InvalidToken_ReturnsFalse() {
        // Given
        String accessToken = "invalid-token";
        ApiException apiException = new ApiException(ErrorCode.KAKAO_AUTH_FAILED);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserResponse.class))
                .thenReturn(Mono.error(apiException));

        // When
        boolean result = kakaoClient.validateToken(accessToken);

        // Then
        assertThat(result).isFalse();
        verify(webClient).get();
    }

    @Test
    @DisplayName("카카오 토큰 검증 - 일반 예외")
    void validateToken_GeneralException_ReturnsFalse() {
        // Given
        String accessToken = "token-with-error";
        RuntimeException generalException = new RuntimeException("General error");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserResponse.class))
                .thenReturn(Mono.error(generalException));

        // When
        boolean result = kakaoClient.validateToken(accessToken);

        // Then
        assertThat(result).isFalse();
        verify(webClient).get();
    }

    // Helper method
    private KakaoUserResponse createMockKakaoUserResponse() {
        KakaoUserResponse mockResponse = mock(KakaoUserResponse.class);
        lenient().when(mockResponse.getKakaoId()).thenReturn("12345");
        lenient().when(mockResponse.getEmail()).thenReturn("test@kakao.com");
        lenient().when(mockResponse.getBirthYear()).thenReturn("1990");
        lenient().when(mockResponse.getGender()).thenReturn("male");
        return mockResponse;
    }
}