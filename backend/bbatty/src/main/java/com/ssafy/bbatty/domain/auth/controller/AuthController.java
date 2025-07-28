package com.ssafy.bbatty.domain.auth.controller;

import com.ssafy.bbatty.domain.auth.constants.AuthConstants;
import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequestDto;
import com.ssafy.bbatty.domain.auth.dto.request.RefreshTokenRequestDto;
import com.ssafy.bbatty.domain.auth.dto.request.SignupCompleteRequestDto;
import com.ssafy.bbatty.domain.auth.dto.request.TeamSelectionRequestDto;
import com.ssafy.bbatty.domain.auth.dto.response.*;
import com.ssafy.bbatty.domain.auth.service.AuthService;
import com.ssafy.bbatty.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponse<KakaoLoginResponseDto>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequestDto request) {
        
        KakaoLoginResponseDto response = authService.kakaoLogin(request);
        
        return ResponseEntity.ok(
            ApiResponse.success(response)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(AuthConstants.JWT_HEADER) String authHeader) {

        String accessToken = authHeader.replace(AuthConstants.JWT_PREFIX, "");
        authService.logout(accessToken);

        return ResponseEntity.ok(
                ApiResponse.success()
        );
    }

    @PostMapping("/select-team")
    public ResponseEntity<ApiResponse<TeamSelectionResponseDto>> selectTeam(
            @Valid @RequestBody TeamSelectionRequestDto request) {

        TeamSelectionResponseDto response = authService.selectTeam(request);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponseDto>> checkNickname(
            @RequestParam String nickname) {

        NicknameCheckResponseDto response = authService.checkNickname(nickname);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @PostMapping("/signup/complete")
    public ResponseEntity<ApiResponse<SignupCompleteResponseDto>> signupComplete(
            @Valid @RequestBody SignupCompleteRequestDto request) {
        
        SignupCompleteResponseDto response = authService.signupComplete(request);
        
        return ResponseEntity.ok(
            ApiResponse.success(response)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        
        TokenResponseDto response = authService.refreshToken(request);
        
        return ResponseEntity.ok(
            ApiResponse.success(response)
        );
    }
}