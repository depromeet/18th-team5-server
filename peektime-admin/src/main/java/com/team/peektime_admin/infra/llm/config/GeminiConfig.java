package com.team.peektime_admin.infra.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiConfig {

    private String apiKey;
    private String model;
    private String endpoint;

    @Bean
    public RestClient geminiRestClient() {
        return RestClient.builder()
                .baseUrl(endpoint)
                .build();
    }
}