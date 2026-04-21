package com.team.peektime_admin.domain.mission.service;

import com.team.peektime_admin.domain.mission.dto.GeneratedMissionDto;
import com.team.peektime_admin.domain.mission.dto.GeneratedMissionsWrapper;
import com.team.peektime_admin.domain.mission.prompt.MissionPromptTemplate;
import com.team.peektime_admin.infra.llm.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionGenerationService {

    private final GeminiClient geminiClient;
    private final JsonMapper jsonMapper;

    public List<GeneratedMissionDto> generateMissions(int count) {
        String prompt = MissionPromptTemplate.generate(count);
        return callGeminiAndParse(prompt);
    }

    public List<GeneratedMissionDto> generateMissionsWithTheme(String theme, int count) {
        String prompt = MissionPromptTemplate.generateWithTheme(theme, count);
        return callGeminiAndParse(prompt);
    }

    private List<GeneratedMissionDto> callGeminiAndParse(String prompt) {
        String response = geminiClient.generateContent(prompt);
        log.info("Gemini 응답: {}", response);

        try {
            GeneratedMissionsWrapper wrapper = jsonMapper.readValue(response, GeneratedMissionsWrapper.class);
            return wrapper.getMissions();
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("미션 생성 응답 파싱에 실패했습니다", e);
        }
    }
}