package com.team.peektime_api.global.infra.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FcmConfig {

    private final FcmProperties fcmProperties;
    private final ResourceLoader resourceLoader;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(getCredentials())
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase 초기화 완료");
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }

    private GoogleCredentials getCredentials() throws IOException {
        // 1. 환경변수 방식 (운영 환경)
        if (StringUtils.hasText(fcmProperties.getCredentials())) {
            InputStream stream = new ByteArrayInputStream(
                    fcmProperties.getCredentials().getBytes(StandardCharsets.UTF_8)
            );
            return GoogleCredentials.fromStream(stream);
        }

        // 2. 파일 경로 방식 (로컬 개발)
        if (StringUtils.hasText(fcmProperties.getCredentialsPath())) {
            InputStream stream = resourceLoader
                    .getResource(fcmProperties.getCredentialsPath())
                    .getInputStream();
            return GoogleCredentials.fromStream(stream);
        }

        throw new IllegalStateException("Firebase credentials 설정이 없습니다.");
    }
}