package com.team.peektime_admin.domain.mission.service;

import com.team.peektime_admin.domain.mission.dto.GeneratedMissionDto;
import com.team.peektime_admin.domain.mission.dto.GeneratedMissionsWrapper;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.prompt.MissionPromptTemplate;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.global.common.enums.*;
import com.team.peektime_admin.infra.llm.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionGenerationService {

    private final GeminiClient geminiClient;
    private final JsonMapper jsonMapper;
    private final SolarTermRepository solarTermRepository;
    private final MissionRepository missionRepository;

    @Transactional
    public List<GeneratedMissionDto> generateMissions(int count) {
        String prompt = MissionPromptTemplate.generate(count);
        List<GeneratedMissionDto> missions = callGeminiAndParse(prompt);
        saveMissions(missions);
        return missions;
    }

    @Transactional
    public List<GeneratedMissionDto> generateMissionsWithTheme(String theme, int count) {
        String prompt = MissionPromptTemplate.generateWithTheme(theme, count);
        List<GeneratedMissionDto> missions = callGeminiAndParse(prompt);
        saveMissions(missions);
        return missions;
    }

    @Transactional
    public List<GeneratedMissionDto> generateMissionsWithSolarTerm(Long solarTermId, int count) {
        SolarTerm solarTerm = solarTermRepository.findById(solarTermId)
                .orElseThrow(() -> new IllegalArgumentException("절기를 찾을 수 없습니다: " + solarTermId));

        String prompt = MissionPromptTemplate.generateWithSolarTerm(solarTerm, count);
        List<GeneratedMissionDto> missions = callGeminiAndParse(prompt);
        saveMissions(missions);
        return missions;
    }

    @Transactional
    public List<GeneratedMissionDto> generateMissionsWithSolarTermAndUserType(Long solarTermId, String userTypeStr, int count) {
        SolarTerm solarTerm = solarTermRepository.findById(solarTermId)
                .orElseThrow(() -> new IllegalArgumentException("절기를 찾을 수 없습니다: " + solarTermId));

        UserType userType = parseEnum(UserType.class, userTypeStr);

        String prompt = MissionPromptTemplate.generateWithSolarTermAndUserType(solarTerm, userType, count);
        List<GeneratedMissionDto> missions = callGeminiAndParse(prompt);
        saveMissions(missions);
        return missions;
    }

    private void saveMissions(List<GeneratedMissionDto> missionDtos) {
        List<Mission> missions = missionDtos.stream()
                .map(this::toEntity)
                .toList();
        missionRepository.saveAll(missions);
        log.info("{}개의 미션이 저장되었습니다.", missions.size());
    }

    private Mission toEntity(GeneratedMissionDto dto) {
        return Mission.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .spaceType(parseEnum(SpaceType.class, dto.getSpaceType()))
                .intensityType(parseEnum(IntensityType.class, dto.getIntensityType()))
                .companionType(parseEnum(CompanionType.class, dto.getCompanionType()))
                .categoryType(parseEnum(CategoryType.class, dto.getCategoryType()))
                .enjoyType(parseEnumOrNull(EnjoyType.class, dto.getEnjoyType()))
                .userType(parseEnumOrNull(UserType.class, dto.getUserType()))
                .build();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(enumClass.getSimpleName() + " 값이 필요합니다.");
        }
        return Enum.valueOf(enumClass, value.toUpperCase());
    }

    private <T extends Enum<T>> T parseEnumOrNull(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 {} 값: {}", enumClass.getSimpleName(), value);
            return null;
        }
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