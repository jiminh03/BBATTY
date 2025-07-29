package com.ssafy.bbatty.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoProperties {

    private String clientId;
    private String clientSecret;
    private String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
    private int timeoutSeconds = 10;
}