package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.domain.auth.client.KakaoClient;
import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequest;
import com.ssafy.bbatty.domain.auth.dto.request.SignupRequest;
import com.ssafy.bbatty.domain.auth.dto.KakaoUserInfo;
import com.ssafy.bbatty.domain.auth.dto.response.AuthResponse;
import com.ssafy.bbatty.domain.auth.dto.response.KakaoUserResponse;
import com.ssafy.bbatty.domain.auth.dto.response.TokenPair;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.entity.UserInfo;
import com.ssafy.bbatty.domain.user.repository.UserInfoRepository;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.domain.user.service.UserService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.constants.Role;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 인증 서비스 - Stateless JWT 방식
 * - 리프레시 토큰은 Redis에 저장하지 않음 (JWT 자체 검증)
 * - 블랙리스트를 통한 토큰 무효화만 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final KakaoClient kakaoClient;
    private final JwtProvider jwtProvider;
    private final AuthCacheService authCacheService;

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final TeamRepository teamRepository;
    private final UserService userService;

    /**
     * 카카오 로그인 처리
     */
    public AuthResponse kakaoLogin(KakaoLoginRequest request) {
        // 1. 카카오 사용자 정보 추출 (외부 API 호출)
        KakaoUserInfo kakaoUserInfo = extractKakaoUserInfo(request.getAccessToken());
        
        // 2. DB 작업은 트랜잭션 내부에서
        return performLogin(kakaoUserInfo);
    }
    
    @Transactional
    protected AuthResponse performLogin(KakaoUserInfo kakaoUserInfo) {
        // 기존 사용자 확인
        Optional<UserInfo> existingUserInfo = userInfoRepository.findByKakaoId(kakaoUserInfo.getKakaoId());

        if (existingUserInfo.isPresent()) {
            // 기존 사용자 로그인
            User user = existingUserInfo.get().getUser();
            
            log.info("카카오 로그인 성공: userId={}, kakaoId={}", user.getId(), kakaoUserInfo.getKakaoId());
            return createAuthResponse(user, AuthResponse::ofLogin);
        } else {
            // 신규 사용자 - 회원가입 필요
            log.info("신규 사용자 로그인 시도: kakaoId={}", kakaoUserInfo.getKakaoId());
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 회원가입 처리
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 1. 카카오 사용자 정보 추출 (외부 API 호출)
        KakaoUserInfo kakaoUserInfo = extractKakaoUserInfo(request.getAccessToken());
        
        // 2. 회원가입 요청 검증
        validateSignupRequest(request, kakaoUserInfo);
        
        // 3. 사용자 및 사용자 정보 생성
        User user = createUserWithInfo(request, kakaoUserInfo);
        
        log.info("회원가입 성공: userId={}, kakaoId={}, teamId={}",
                user.getId(), kakaoUserInfo.getKakaoId(), user.getTeamId());
        
        return createAuthResponse(user, AuthResponse::ofSignup);
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
     * 닉네임 중복 확인
     */
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdraw(Long userId) {
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // UserService의 deleteUser 메서드를 통해 관련 데이터 삭제
        userService.deleteUser(userId);

        log.info("회원 탈퇴 완료: userId={}", userId);
    }

    // Private 헬퍼 메서드들
    
    /**
     * 카카오 사용자 정보 추출 및 검증
     */
    private KakaoUserInfo extractKakaoUserInfo(String accessToken) {
        KakaoUserResponse kakaoUser = kakaoClient.getUserInfoFromKakao(accessToken);
        validateKakaoUserInfo(kakaoUser);
        
        return KakaoUserInfo.builder()
                .kakaoId(kakaoUser.getKakaoId())
                .email(kakaoUser.getEmail())
                .gender(Gender.fromKakaoValue(kakaoUser.getGender()))
                .birthYear(Integer.parseInt(kakaoUser.getBirthYear()))
                .build();
    }
    
    /**
     * 회원가입 요청 검증
     */
    private void validateSignupRequest(SignupRequest request, KakaoUserInfo kakaoInfo) {
        // 중복 가입 확인
        if (userInfoRepository.existsByKakaoId(kakaoInfo.getKakaoId())) {
            throw new ApiException(ErrorCode.DUPLICATE_SIGNUP);
        }
        
        // 닉네임 중복 확인 (서버에서 최종 검증)
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }
    
    /**
     * 사용자 및 사용자 정보 생성
     */
    private User createUserWithInfo(SignupRequest request, KakaoUserInfo kakaoInfo) {
        // 팀 존재 확인
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ApiException(ErrorCode.TEAM_NOT_FOUND));
        
        // 사용자 생성
        User user = User.createUser(
                request.getNickname(),
                team,
                kakaoInfo.getGender(),
                kakaoInfo.getBirthYear(),
                request.getProfileImg(),
                request.getIntroduction(),
                Role.USER
        );
        userRepository.save(user);
        
        // 사용자 상세 정보 생성
        UserInfo userInfo = UserInfo.createUserInfo(user, kakaoInfo.getKakaoId(), kakaoInfo.getEmail());
        userInfoRepository.save(userInfo);
        
        return user;
    }
    
    /**
     * 공통 인증 응답 생성
     */
    private AuthResponse createAuthResponse(User user, BiFunction<TokenPair, AuthResponse.UserProfile, AuthResponse> responseFactory) {
        TokenPair tokens = createTokenPair(user);
        AuthResponse.UserProfile userProfile = createUserProfileResponse(user);
        return responseFactory.apply(tokens, userProfile);
    }

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
        int userAge = user.getAge(); // 한 번만 계산
        
        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                userAge,
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

    private AuthResponse.UserProfile createUserProfileResponse(User user) {
        int userAge = user.getAge(); // 한 번만 계산
        
        return AuthResponse.UserProfile.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .teamId(user.getTeamId())
                .age(userAge)
                .gender(user.getGender().name())
                .build();
    }
}