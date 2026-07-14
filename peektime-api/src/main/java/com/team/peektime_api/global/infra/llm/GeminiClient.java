package com.team.peektime_api.global.infra.llm;

import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.llm.config.GeminiConfig;
import com.team.peektime_api.global.infra.llm.dto.GeminiRequest;
import com.team.peektime_api.global.infra.llm.dto.GeminiResponse;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    // 필드명이 빈 이름(geminiRestClient)과 일치해야 adminRestClient와 구분되어 주입된다
    private final RestClient geminiRestClient;
    private final GeminiConfig config;

    public String generateContent(String prompt) {
        GeminiRequest request = GeminiRequest.of(prompt);

        try {
            GeminiResponse response = geminiRestClient.post()
                    .uri("/{model}:generateContent?key={apiKey}", config.getModel(), config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.getText().isBlank()) {
                throw new BusinessException(ErrorCode.MISSION_GENERATION_FAILED);
            }
            return response.getText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.MISSION_GENERATION_FAILED);
        }
    }
}