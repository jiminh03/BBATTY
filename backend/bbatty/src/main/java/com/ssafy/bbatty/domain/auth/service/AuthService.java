package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.domain.auth.client.KakaoClient;
import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequest;
import com.ssafy.bbatty.domain.auth.dto.request.SignupRequest;
import com.ssafy.bbatty.domain.auth.dto.response.AuthResponse;
import com.ssafy.bbatty.domain.auth.dto.response.KakaoUserResponse;
import com.ssafy.bbatty.domain.auth.dto.response.TokenPair;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.entity.UserInfo;
import com.ssafy.bbatty.domain.user.repository.UserInfoRepository;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 인증 서비스 - Stateless JWT 방식
 * - 리프레시 토큰은 Redis에 저장하지 않음 (JWT 자체 검증)
 * - 블랙리스트를 통한 토큰 무효화만 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final KakaoClient kakaoClient;
    private final JwtProvider jwtProvider;
    private final AuthCacheService authCacheService;

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final TeamRepository teamRepository;

    /**
     * 카카오 로그인 처리
     */
    @Transactional
    public AuthResponse kakaoLogin(KakaoLoginRequest request) {
        // 1. 카카오 사용자 정보 조회
        KakaoUserResponse kakaoUser = kakaoClient.getUserInfo(request.getAccessToken());

        // 2. 필수 정보 검증
        validateKakaoUserInfo(kakaoUser);

        // 3. 기존 사용자 확인
        Optional<UserInfo> existingUserInfo = userInfoRepository.findByKakaoId(kakaoUser.getKakaoId());

        if (existingUserInfo.isPresent()) {
            // 기존 사용자 로그인
            User user = existingUserInfo.get().getUser();
            TokenPair tokens = createTokenPair(user);
            AuthResponse.UserInfo userInfo = createUserInfoResponse(user);

            log.info("카카오 로그인 성공: userId={}, kakaoId={}", user.getId(), kakaoUser.getKakaoId());
            return AuthResponse.ofLogin(tokens, userInfo);
        } else {
            // 신규 사용자 - 회원가입 필요
            log.info("신규 사용자 로그인 시도: kakaoId={}", kakaoUser.getKakaoId());
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 회원가입 처리
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 1. 카카오 사용자 정보 조회
        KakaoUserResponse kakaoUser = kakaoClient.getUserInfo(request.getAccessToken());

        // 2. 필수 정보 검증
        validateKakaoUserInfo(kakaoUser);

        // 3. 중복 가입 확인
        if (userInfoRepository.existsByKakaoId(kakaoUser.getKakaoId())) {
            throw new ApiException(ErrorCode.DUPLICATE_SIGNUP);
        }

        // 4. 닉네임 중복 확인
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 5. 팀 존재 확인
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ApiException(ErrorCode.TEAM_NOT_FOUND));

        // 6. 사용자 생성
        User user = User.builder()
                .team(team)
                .nickname(request.getNickname())
                .gender(Gender.fromKakaoValue(kakaoUser.getGender()))
                .birthYear(Integer.parseInt(kakaoUser.getBirthYear()))
                .profileImg(request.getProfileImg())
                .introduction(request.getIntroduction())
                .build();

        userRepository.save(user);

        // 8. 사용자 상세 정보 생성
        UserInfo userInfo = UserInfo.builder()
                .user(user)
                .kakaoId(kakaoUser.getKakaoId())
                .email(kakaoUser.getEmail())
                .build();

        userInfoRepository.save(userInfo);

        // 9. JWT 토큰 발급
        TokenPair tokens = createTokenPair(user);
        AuthResponse.UserInfo responseUserInfo = createUserInfoResponse(user);

        log.info("회원가입 성공: userId={}, kakaoId={}, teamId={}",
                user.getId(), kakaoUser.getKakaoId(), team.getId());

        return AuthResponse.ofSignup(tokens, responseUserInfo);
    }

    /**
     * 토큰 갱신 (Stateless 방식)
     */
    @Transactional
    public TokenPair refreshToken(String refreshToken) {
        // 1. Refresh Token 검증 (JWT 자체 검증)
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 블랙리스트 확인 (Redis 조회)
        if (authCacheService.isTokenBlacklisted(refreshToken)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        // 3. 사용자 정보 조회 (DB에서 최신 정보 확인)
        Long userId = jwtProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 4. 기존 Refresh Token 블랙리스트 추가 (재사용 방지)
        authCacheService.blacklistToken(refreshToken, jwtProvider.getExpiration(refreshToken));

        // 5. 새 토큰 쌍 발급
        TokenPair newTokens = createTokenPair(user);

        log.info("토큰 갱신 성공: userId={}", userId);
        return newTokens;
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // Access Token 블랙리스트 추가
        if (accessToken != null && jwtProvider.validateAccessToken(accessToken)) {
            authCacheService.blacklistToken(accessToken, jwtProvider.getExpiration(accessToken));
        }

        // Refresh Token 블랙리스트 추가
        if (refreshToken != null && jwtProvider.validateRefreshToken(refreshToken)) {
            authCacheService.blacklistToken(refreshToken, jwtProvider.getExpiration(refreshToken));
        }

        log.info("로그아웃 완료");
    }

    // Private 헬퍼 메서드들

    private void validateKakaoUserInfo(KakaoUserResponse kakaoUser) {
        if (kakaoUser.getEmail() == null) {
            throw new ApiException(ErrorCode.KAKAO_EMAIL_INFO_REQUIRED);
        }

        if (kakaoUser.getBirthYear() == null) {
            throw new ApiException(ErrorCode.KAKAO_BIRTH_INFO_REQUIRED);
        }

        if (kakaoUser.getGender() == null) {
            throw new ApiException(ErrorCode.KAKAO_GENDER_INFO_REQUIRED);
        }

        // 생년 형식 검증 (YYYY)
        try {
            int birthYear = Integer.parseInt(kakaoUser.getBirthYear());
            if (birthYear < 1900 || birthYear > LocalDate.now().getYear()) {
                throw new ApiException(ErrorCode.KAKAO_BIRTH_INFO_INVALID);
            }
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.KAKAO_BIRTH_INFO_INVALID);
        }
    }

    /**
     * JWT 토큰 쌍 생성 (순수 JWT, Redis 저장 없음)
     */
    private TokenPair createTokenPair(User user) {
        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getAge(),
                user.getGender().name(),
                user.getTeamId()
        );

        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        return TokenPair.of(
                accessToken,
                refreshToken,
                jwtProvider.getExpiration(accessToken),
                jwtProvider.getExpiration(refreshToken)
        );
    }

    private AuthResponse.UserInfo createUserInfoResponse(User user) {
        return AuthResponse.UserInfo.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .teamId(user.getTeamId())
                .teamName(user.getTeam().getName())
                .introduction(user.getIntroduction())
                .age(user.getAge())
                .gender(user.getGender().name())
                .build();
    }
}