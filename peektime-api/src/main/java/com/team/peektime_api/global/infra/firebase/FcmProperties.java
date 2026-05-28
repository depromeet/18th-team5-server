package com.team.peektime_api.global.infra.firebase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "firebase")
public class FcmProperties {

    private String credentialsPath;  // 파일 경로 방식
    private String credentials;      // 환경변수 방식 (JSON 문자열)
}