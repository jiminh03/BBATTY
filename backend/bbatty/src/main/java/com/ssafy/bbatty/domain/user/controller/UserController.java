package com.ssafy.bbatty.domain.user.controller;

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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponseDto response = userService.getMyProfile(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 다른 사용자 프로필 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponseDto response = userService.getUserProfile(userId, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프로필 수정
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateProfile(
            @RequestBody UserUpdateRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponseDto response = userService.updateProfile(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 닉네임 중복 체크
     */
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<Boolean>> checkNicknameAvailability(
            @RequestParam String nickname,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        boolean available = userService.isNicknameAvailable(nickname, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(available));
    }

    /**
     * 프라이버시 설정 업데이트
     */
    @PutMapping("/me/privacy")
    public ResponseEntity<ApiResponse<Void>> updatePrivacySettings(
            @RequestParam Boolean postsPublic,
            @RequestParam Boolean statsPublic, 
            @RequestParam Boolean attendanceRecordsPublic,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.updatePrivacySettings(
            userPrincipal.getUserId(), 
            postsPublic, 
            statsPublic, 
            attendanceRecordsPublic
        );
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.deleteUser(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
