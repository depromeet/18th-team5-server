package com.team.peektime_admin.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Bean
    public RestClient apiRestClient() {
        return RestClient.builder()
                .baseUrl(apiBaseUrl)
                .build();
    }
}