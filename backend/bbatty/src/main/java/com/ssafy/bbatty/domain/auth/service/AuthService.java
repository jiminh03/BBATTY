package com.ssafy.bbatty.domain.auth.service;

import com.ssafy.bbatty.domain.auth.constants.AuthConstants;
import com.ssafy.bbatty.domain.auth.dto.external.KakaoUserInfoDto;
import com.ssafy.bbatty.domain.auth.dto.request.KakaoLoginRequestDto;
import com.ssafy.bbatty.domain.auth.dto.request.RefreshTokenRequestDto;
import com.ssafy.bbatty.domain.auth.dto.request.SignupCompleteRequestDto;
import com.ssafy.bbatty.domain.auth.dto.request.TeamSelectionRequestDto;
import com.ssafy.bbatty.domain.auth.dto.response.*;
import com.ssafy.bbatty.domain.user.dto.response.UserDto;
import com.ssafy.bbatty.domain.auth.exception.AuthExceptions;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.entity.UserInfo;
import com.ssafy.bbatty.domain.user.repository.UserInfoRepository;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.util.JwtUtil;
import com.ssafy.bbatty.global.util.RedisUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final TeamRepository teamRepository;

    private final KakaoAuthService kakaoAuthService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    public KakaoLoginResponseDto kakaoLogin(KakaoLoginRequestDto request) {
        KakaoUserInfoDto kakaoUser = kakaoAuthService.getUserInfo(request.getAccessToken());
        
        UserInfo userInfo = userInfoRepository.findByKakaoId(kakaoUser.getKakaoId())
                .orElse(null);
        
        if (userInfo != null) {
            User user = userInfo.getUser();
            String accessToken = jwtUtil.generateAccessToken(user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());
            
            storeRefreshToken(user.getId(), refreshToken);
            
            TokenResponseDto tokens = TokenResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .build();
            
            return KakaoLoginResponseDto.forExistingUser(tokens, user);
        } else {
            return KakaoLoginResponseDto.forNewUser(
                kakaoUser.getKakaoId(), 
                kakaoUser.getEmail(),
                kakaoUser.getBirthyear(),
                kakaoUser.getBirthday(),
                kakaoUser.getGender()
            );
        }
    }

    public SignupCompleteResponseDto signupComplete(SignupCompleteRequestDto request) {
        // 중복 가입 확인
        if (userInfoRepository.existsByKakaoId(request.getKakaoId())) {
            throw new AuthExceptions.DuplicateSignupException();
        }
        
        // 닉네임 중복 확인
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new AuthExceptions.DuplicateNicknameException();
        }
        
        // 팀 정보 확인
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(AuthExceptions.TeamNotFoundException::new);
        
        // 카카오 정보로부터 나이와 성별 계산
        int age = User.calculateAge(request.getBirthyear(), request.getBirthday());
        User.Gender gender = User.parseGender(request.getGender());
        
        // User 엔티티 생성
        User user = User.builder()
                .nickname(request.getNickname())
                .gender(gender)
                .age(age)
                .team(team)
                .introduction(request.getIntroduction())
                .profileImg(request.getProfileImg())
                .role(User.Role.USER)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // UserInfo 엔티티 생성
        UserInfo userInfo = UserInfo.createUserInfo(savedUser, request.getKakaoId(), request.getEmail());
        userInfoRepository.save(userInfo);
        
        // JWT 토큰 생성 및 저장
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());
        
        storeRefreshToken(savedUser.getId(), refreshToken);
        
        TokenResponseDto tokens = TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
        
        // UserDto 생성
        UserDto userDto = UserDto.from(savedUser);
        
        return SignupCompleteResponseDto.success(tokens, userDto);
    }

    public TeamSelectionResponseDto selectTeam(TeamSelectionRequestDto request) {
        // 팀 존재 확인
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(AuthExceptions.TeamNotFoundException::new);

        return TeamSelectionResponseDto.of(team.getId(), team.getName());
    }

    @Transactional(readOnly = true)
    public NicknameCheckResponseDto checkNickname(String nickname) {
        boolean isAvailable = !userRepository.existsByNickname(nickname);
        
        if (isAvailable) {
            return NicknameCheckResponseDto.available();
        } else {
            return NicknameCheckResponseDto.unavailable();
        }
    }

    private void storeRefreshToken(Long userId, String refreshToken) {
        String key = AuthConstants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        Duration validity = Duration.ofDays(14); // refreshTokenValidity와 동일하게
        redisUtil.setValueWithExpiration(key, refreshToken, validity);
    }

    // Refresh Token 검증 시 Redis에서 확인
    public TokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new AuthExceptions.InvalidTokenException();
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        // Redis에 저장된 Refresh Token과 비교
        String storedToken = redisUtil.getValue(
                AuthConstants.REDIS_REFRESH_TOKEN_PREFIX + userId, String.class
        );

        if (!refreshToken.equals(storedToken)) {
            throw new AuthExceptions.InvalidTokenException("Refresh Token이 일치하지 않습니다.");
        }

        // 새 토큰 생성 및 저장
        String newAccessToken = jwtUtil.generateAccessToken(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        storeRefreshToken(userId, newRefreshToken);

        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .build();
    }

    public void logout(String accessToken) {
        if (!jwtUtil.validateToken(accessToken)) {
            throw new AuthExceptions.InvalidTokenException();
        }

        // 토큰을 블랙리스트에 추가
        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        String blacklistKey = AuthConstants.REDIS_BLACKLIST_PREFIX + accessToken;

        // Access Token의 남은 만료시간만큼 블랙리스트에 저장
        Claims claims = jwtUtil.parseToken(accessToken);
        Date expiration = claims.getExpiration();
        long ttl = expiration.getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            redisUtil.setValueWithExpiration(blacklistKey, "BLACKLISTED", Duration.ofMillis(ttl));
        }

        // Refresh Token도 Redis에서 삭제
        String refreshTokenKey = AuthConstants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        redisUtil.deleteKey(refreshTokenKey);
    }
}