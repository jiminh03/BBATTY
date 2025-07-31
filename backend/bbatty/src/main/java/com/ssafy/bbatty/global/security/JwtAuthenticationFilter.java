package com.ssafy.bbatty.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * - Authorization 헤더에서 토큰 추출
 * - 토큰 유효성 검증
 * - Redis 블랙리스트 확인
 * - SecurityContext에 사용자 정보 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && jwtProvider.validateAccessToken(token)) {
                // Redis 블랙리스트 확인
                if (isTokenBlacklisted(token)) {
                    log.warn("블랙리스트에 등록된 토큰입니다: {}", token);
                } else {
                    // 토큰에서 사용자 정보 추출
                    Long userId = jwtProvider.getUserId(token);
                    int age = jwtProvider.getAge(token);
                    String gender = jwtProvider.getGender(token);
                    Long teamId = jwtProvider.getTeamId(token);

                    // UserPrincipal 생성 및 SecurityContext 설정
                    UserPrincipal userPrincipal = new UserPrincipal(userId, age, gender, teamId);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("사용자 인증 완료: userId={}, teamId={}", userId, teamId);
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return jwtProvider.extractToken(bearerToken);
    }

    /**
     * 토큰이 블랙리스트에 등록되어 있는지 확인
     */
    private boolean isTokenBlacklisted(String token) {
        String key = "blacklist:token:" + token;
        return redisTemplate.hasKey(key);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 인증이 필요 없는 경로들
        return path.startsWith("/auth/") ||
                path.startsWith("/public/") ||
                path.startsWith("/health") ||
                path.startsWith("/actuator/") ||
                path.equals("/");
    }
}