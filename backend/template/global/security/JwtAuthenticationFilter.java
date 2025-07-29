package com.ssafy.bbatty.global.security;

import com.ssafy.bbatty.domain.auth.constants.AuthConstants;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    UserPrincipal userPrincipal = new UserPrincipal(
                            user.getId(),
                            user.getNickname(),
                            user.getRole().name()
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    userPrincipal.getAuthorities()
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT 인증 성공 - 사용자 ID: {}, 닉네임: {}", userId, user.getNickname());
                }
            }
        } catch (Exception e) {
            log.error("JWT 필터에서 예외 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 인증이 필요없는 경로들
        return path.startsWith("/api/auth/") ||
                path.startsWith("/api/teams/") ||
                path.startsWith("/ws/") ||
                path.startsWith("/h2-console/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/swagger-ui/");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConstants.JWT_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AuthConstants.JWT_PREFIX)) {
            return bearerToken.substring(AuthConstants.JWT_PREFIX.length());
        }

        return null;
    }
}