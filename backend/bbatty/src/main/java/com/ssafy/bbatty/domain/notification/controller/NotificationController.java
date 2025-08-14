package com.ssafy.bbatty.domain.notification.controller;

import com.ssafy.bbatty.domain.notification.dto.request.FCMTokenRequest;
import com.ssafy.bbatty.domain.notification.dto.response.NotificationSettingResponse;
import com.ssafy.bbatty.domain.notification.service.NotificationSettingService;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationSettingService notificationSettingService;

    /**
     * FCM 토큰 등록/업데이트
     */
    @PutMapping("/fcm-token")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> registerFCMToken(
            @Valid @RequestBody FCMTokenRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getUserId();
        NotificationSettingResponse response = notificationSettingService.registerFCMToken(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}