package com.ssafy.bbatty.domain.notification.controller;

import com.ssafy.bbatty.domain.notification.dto.request.FCMTokenRequest;
import com.ssafy.bbatty.domain.notification.dto.request.NotificationSettingRequest;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationSettingService notificationSettingService;

    /**
     * FCM 토큰 등록/업데이트
     */
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> registerFCMToken(
            @Valid @RequestBody FCMTokenRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getUserId();
        NotificationSettingResponse response = notificationSettingService.registerFCMToken(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 알림 설정 업데이트
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateNotificationSettings(
            @RequestBody NotificationSettingRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getUserId();
        NotificationSettingResponse response = notificationSettingService.updateNotificationSettings(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 알림 설정 조회
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getNotificationSettings(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getUserId();
        NotificationSettingResponse response = notificationSettingService.getNotificationSettings(userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * FCM 토큰 삭제
     */
    @DeleteMapping("/token")
    public ResponseEntity<ApiResponse<Void>> deleteFCMToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getUserId();
        notificationSettingService.deleteFCMToken(userId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}