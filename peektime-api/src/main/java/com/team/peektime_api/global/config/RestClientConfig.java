package com.team.peektime_api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    // 두 타임아웃은 순차 구간(연결 수립 → 응답 대기)이라 요청당 최악 소요는 합산 5초.
    // 이 값은 outbox 폴러의 배치 최악 시간(30건 × 5초 = 150초)과 claim 리스(×2 = 300초)
    // 산정의 기준이므로, 바꾸면 OutboxEvent.CLAIM_LEASE_SECONDS도 같이 재계산할 것
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    @Value("${admin.base-url}")
    private String adminBaseUrl;

    @Bean
    public RestClient adminRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(adminBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}