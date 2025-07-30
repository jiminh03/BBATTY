package com.ssafy.bbatty.global.security;

import io.jsonwebtoken.security.SignatureAlgorithm;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.Signature;

/**
 * JWT 유틸리티 클래스
 * 토큰 블랙리스트와 기기 관리 기능 포함
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // 토큰 만료 시간
    private static final long ACCESS_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

}
