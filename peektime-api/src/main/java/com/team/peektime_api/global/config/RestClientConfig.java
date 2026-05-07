package com.team.peektime_api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${admin.base-url}")
    private String adminBaseUrl;

    @Bean
    public RestClient adminRestClient() {
        return RestClient.builder()
                .baseUrl(adminBaseUrl)
                .build();
    }
}