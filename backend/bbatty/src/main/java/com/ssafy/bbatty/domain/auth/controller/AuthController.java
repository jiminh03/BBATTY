package com.ssafy.bbatty.domain.auth.controller;

import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequest;
import com.ssafy.bbatty.domain.auth.dto.request.SignupRequest;
import com.ssafy.bbatty.domain.auth.dto.response.AuthResponse;
import com.ssafy.bbatty.domain.auth.dto.response.NicknameCheckResponse;
import com.ssafy.bbatty.domain.auth.dto.response.TokenPair;
import com.ssafy.bbatty.domain.auth.service.AuthService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.SuccessCode;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.s3.S3Service;
import com.ssafy.bbatty.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * - 카카오 로그인/회원가입
 * - 토큰 갱신
 * - 회원 탈퇴
 *
 * 📱 프론트엔드 개발자를 위한 API 가이드:
 * 1. POST /api/auth/login - 카카오 로그인 (기존 사용자)
 * 2. POST /api/auth/signup - 회원가입 (신규 사용자)
 * 3. POST /api/auth/refresh - 토큰 갱신
 * 4. GET /api/auth/check-nickname - 닉네임 중복 확인
 * 5. POST /api/auth/profile/presigned-url - 프로필 이미지 업로드용 Presigned URL 생성
 * 6. DELETE /api/auth/withdraw - 회원 탈퇴
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final S3Service s3Service;

    /**
     * 카카오 로그인
     *
     * 📝 프론트 처리:
     * - 성공: AuthResponse 받아서 토큰 저장 후 홈 화면
     * - 실패(USER_NOT_FOUND): 회원가입 화면으로 이동
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request
    ) {
        AuthResponse response = authService.kakaoLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 회원가입
     *
     * 📝 프론트 처리:
     * - 카카오 토큰 + 추가 정보(팀, 닉네임 등) 전송
     * - 성공: AuthResponse 받아서 토큰 저장 후 온보딩 완료
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(SuccessCode.SUCCESS_CREATED, response));
    }

    /**
     * 토큰 갱신
     *
     * 📝 프론트 처리:
     * - X-Refresh-Token 헤더에 리프레시 토큰 포함
     *   (X-refresh-token은 HTTP 헤더에서 리프레시 토큰을 전달하는데 사용되는 커스텀 헤더)
     * - 새로운 TokenPair 받아서 기존 토큰 교체
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenPair>> refreshToken(
            HttpServletRequest request
    ) {
        String refreshToken = extractRefreshToken(request);
        TokenPair newTokens = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(newTokens));
    }

    /**
     * 닉네임 중복 확인
     * 
     * 📝 프론트 처리:
     * - 닉네임 입력 후 중복 확인 버튼 클릭 시 호출
     * - available: 사용 가능 여부, message: 안내 메시지
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(
            @RequestParam String nickname
    ) {
        boolean isAvailable = authService.isNicknameAvailable(nickname);
        NicknameCheckResponse response = isAvailable 
            ? NicknameCheckResponse.available()
            : NicknameCheckResponse.unavailable();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 회원 탈퇴
     *
     * 📝 프론트 처리:
     * - Authorization 헤더 포함하여 인증된 사용자만 탈퇴 가능
     * - 성공 후 모든 토큰 삭제하고 로그인 화면으로
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            HttpServletRequest request
    ) {
        String accessToken = jwtProvider.extractToken(request.getHeader("Authorization"));
        Long userId = jwtProvider.getUserId(accessToken);
        
        authService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 프로필 이미지 업로드를 위한 Presigned URL 생성
     * 
     * 📝 프론트 처리:
     * - 회원가입 시 프로필 이미지 업로드를 위해 사용
     * - filename 파라미터로 업로드할 파일명 전송
     * - 응답으로 받은 uploadUrl로 PUT 요청하여 이미지 업로드
     * - fileUrl을 SignupRequest의 profileImageUrl에 포함하여 회원가입 요청
     * - 해당 메서드는 토큰 필요 없음
     */
    @PostMapping("/profile/presigned-url")
    public ResponseEntity<ApiResponse<S3Service.PresignedUrlResponse>> generateProfilePresignedUrl(
            @RequestParam String filename) {
        
        try {
            S3Service.PresignedUrlResponse response = s3Service.generatePresignedUploadUrlWithPath("profiles", filename);
            
            return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus())
                    .body(ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, response));
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_FILE_PATH);
        }
    }

    /**
     * 리프레시 토큰 추출 (헤더에서만)
     * 보안상 URL 파라미터는 로그에 노출될 위험이 있어 제외
     */
    private String extractRefreshToken(HttpServletRequest request) {
        String refreshToken = request.getHeader("X-Refresh-Token");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_MISSING);
        }
        return refreshToken;
    }
}