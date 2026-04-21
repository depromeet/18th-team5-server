package com.team.peektime_admin.infra.llm;

import com.team.peektime_admin.infra.llm.config.GeminiConfig;
import com.team.peektime_admin.infra.llm.dto.GeminiRequest;
import com.team.peektime_admin.infra.llm.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final RestClient geminiRestClient;
    private final GeminiConfig config;

    public String generateContent(String prompt) {
        GeminiRequest request = GeminiRequest.of(prompt);

        GeminiResponse response = geminiRestClient.post()
                .uri("/{model}:generateContent?key={apiKey}", config.getModel(), config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null) {
            throw new RuntimeException("Gemini API 응답이 없습니다");
        }

        return response.getText();
    }
}