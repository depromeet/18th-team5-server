package com.team.peektime_api.global.infra.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {

    private List<Content> contents;
    private GenerationConfig generationConfig;

    public static GeminiRequest of(String prompt) {
        Content content = new Content(List.of(new Part(prompt)));
        // 짧은 미션 1개 생성이라 추론(thinking)이 불필요한데, gemini-2.5-flash는 기본으로 켜져 있어
        // 응답 지연의 대부분을 차지함. thinkingBudget 0으로 비활성화해 응답 속도 개선
        GenerationConfig config = new GenerationConfig("application/json", 8192, new ThinkingConfig(0));
        return new GeminiRequest(List.of(content), config);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        private String responseMimeType;
        private int maxOutputTokens;
        private ThinkingConfig thinkingConfig;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThinkingConfig {
        private int thinkingBudget;
    }
}