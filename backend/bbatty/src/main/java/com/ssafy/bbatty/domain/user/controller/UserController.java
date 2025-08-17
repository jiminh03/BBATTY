package com.ssafy.bbatty.domain.user.controller;

import com.ssafy.bbatty.domain.auth.dto.response.NicknameCheckResponse;
import com.ssafy.bbatty.domain.notification.dto.request.NotificationSettingRequest;
import com.ssafy.bbatty.domain.user.dto.response.UserBadgeResponse;
import com.ssafy.bbatty.domain.user.dto.request.PrivacyUpdateRequestDto;
import com.ssafy.bbatty.domain.user.dto.request.UserUpdateRequestDto;
import com.ssafy.bbatty.domain.user.dto.response.UserResponseDto;
import com.ssafy.bbatty.domain.user.service.UserService;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 프로필 조회 (본인 또는 다른 사용자)
     * userId가 없으면 본인 프로필, 있으면 해당 사용자 프로필 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserProfile(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long targetUserId = (userId != null) ? userId : userPrincipal.getUserId();
        UserResponseDto response = userService.getUserProfile(targetUserId, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프로필 수정
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateProfile(
            @RequestBody UserUpdateRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponseDto response = userService.updateProfile(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 게시글 목록 조회
     * userId가 없으면 본인 프로필, 있으면 해당 사용자 프로필 조회
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Object>> getUserPosts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long targetUserId = (userId != null) ? userId : userPrincipal.getUserId();
        Object response = userService.getUserPosts(targetUserId, userPrincipal.getUserId(), cursor);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 직관 승률 조회
     * userId가 없으면 본인 프로필, 있으면 해당 사용자 프로필 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getUserStats(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String season,        // "total", "2025", "2024" 등 (디폴트: 현재 연도)
            @RequestParam(required = false) String type,          // "basic", "streak", "stadium", "opponent", "dayOfWeek", "homeAway"
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long targetUserId = (userId != null) ? userId : userPrincipal.getUserId();
        Object response = userService.getUserStats(targetUserId, userPrincipal.getUserId(), season, type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 뱃지 조회
     * userId가 없으면 본인 프로필, 있으면 해당 사용자 프로필 조회
     */
    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<UserBadgeResponse>> getUserBadges(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String season,        // "2025", "2024" 등 (디폴트: 현재 연도)
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long targetUserId = (userId != null) ? userId : userPrincipal.getUserId();
        UserBadgeResponse response = userService.getUserBadges(targetUserId, season);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 직관 기록 조회 (무한 스크롤링용)
     * userId가 없으면 본인 프로필, 있으면 해당 사용자 프로필 조회
     */
    @GetMapping("/attendance-records")
    public ResponseEntity<ApiResponse<Object>> getUserAttendanceRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String season,        // "total" or "2025" (시즌)
            @RequestParam(required = false) Long cursor,          // 무한 스크롤링용
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long targetUserId = (userId != null) ? userId : userPrincipal.getUserId();
        Object response = userService.getUserAttendanceRecords(targetUserId, userPrincipal.getUserId(), season, cursor);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 직관 기록이 있는 년도 목록 조회
     * userId가 없으면 본인 프로필, 있으면 해당 사용자 프로필 조회
     */
    @GetMapping("/attendance-years")
    public ResponseEntity<ApiResponse<Object>> getUserAttendanceYears(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long targetUserId = (userId != null) ? userId : userPrincipal.getUserId();
        Object response = userService.getUserAttendanceYears(targetUserId, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 닉네임 중복 체크 (프로필 수정용)
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNicknameAvailability(
            @RequestParam String nickname,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        boolean available = userService.isNicknameAvailable(nickname, userPrincipal.getUserId());
        NicknameCheckResponse response = available 
            ? NicknameCheckResponse.available()
            : NicknameCheckResponse.unavailable();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프라이버시 설정 업데이트
     */
    @PutMapping("/privacy-setting")
    public ResponseEntity<ApiResponse<Void>> updatePrivacySettings(
            @RequestBody PrivacyUpdateRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.updatePrivacySettings(
            userPrincipal.getUserId(), 
            request.getPostsPublic(), 
            request.getStatsPublic(), 
            request.getAttendanceRecordsPublic()
        );
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 알림 설정 업데이트
     */
    @PutMapping("/notification-setting")
    public ResponseEntity<ApiResponse<Void>> updateNotificationSettings(
            @RequestBody NotificationSettingRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.updateNotificationSettings(
            userPrincipal.getUserId(),
            request.getTrafficSpikeAlertEnabled()
        );
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/withdrawal")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.deleteUser(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
