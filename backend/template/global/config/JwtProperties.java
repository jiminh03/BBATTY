package com.ssafy.bbatty.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private Duration accessTokenValidity = Duration.ofMinutes(30);
    private Duration refreshTokenValidity = Duration.ofDays(14);
    private String issuer = "bbatty";
    private String header = "Authorization";
    private String prefix = "Bearer ";
}