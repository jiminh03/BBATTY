package com.ssafy.bbatty.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security의 UserDetails 구현체
 * JWT 토큰에서 추출한 사용자 정보를 SecurityContext에 저장
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final int age;
    private final String gender;
    private final Long teamId;

    public UserPrincipal(Long userId, int age, String gender, Long teamId) {
        this.userId = userId;
        this.age = age;
        this.gender = gender;
        this.teamId = teamId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        // JWT 기반 인증이므로 패스워드 불필요
        return null;
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }
}
