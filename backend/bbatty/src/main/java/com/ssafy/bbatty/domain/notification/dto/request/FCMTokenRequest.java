package com.ssafy.bbatty.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FCMTokenRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @Size(min = 100, max = 4096, message = "FCM 토큰 길이가 유효하지 않습니다.")
    @Pattern(regexp = "^[A-Za-z0-9_:-]+$", message = "FCM 토큰 형식이 유효하지 않습니다.")
    private String fcmToken;

    @Size(max = 100, message = "디바이스 ID가 너무 깁니다.")
    private String deviceId;
    
    @Pattern(regexp = "^(ANDROID|IOS)$", message = "디바이스 타입은 ANDROID 또는 IOS만 허용됩니다.")
    private String deviceType;
}