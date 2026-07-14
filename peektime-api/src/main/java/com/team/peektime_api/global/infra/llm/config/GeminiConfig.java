package com.team.peektime_api.global.infra.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    // LLM 생성 응답은 일반 API보다 오래 걸림
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private String apiKey;
    private String model;
    private String endpoint;

    @Bean
    public RestClient geminiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(endpoint)
                .requestFactory(requestFactory)
                .build();
    }

    // LLM 엔드포인트 전용 실행기. 톰캣 워커는 요청을 여기 넘기고 즉시 반납되며,
    // Gemini 응답 대기(최대 30초)는 요청당 생성되는 가상 스레드가 담당한다
    @Bean
    public ExecutorService llmExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}