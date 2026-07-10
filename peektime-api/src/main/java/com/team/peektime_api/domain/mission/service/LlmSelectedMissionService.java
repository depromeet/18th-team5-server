package com.team.peektime_api.domain.mission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.dto.GeneratedSelectedMissionDto;
import com.team.peektime_api.domain.mission.dto.LlmSelectedMissionResponse;
import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.prompt.LlmSelectedMissionPromptTemplate;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
import com.team.peektime_api.global.common.enums.SpaceType;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.llm.GeminiClient;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

// LLM 호출(최대 30초) 동안 DB 커넥션을 점유하지 않도록 트랜잭션을 걸지 않는다
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmSelectedMissionService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final SolarTermRepository solarTermRepository;

    public LlmSelectedMissionResponse generateSelectedMission(SelectedMissionRequest filter) {
        SolarTerm currentSolarTerm = solarTermRepository.findByDate(LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));

        String prompt = LlmSelectedMissionPromptTemplate.generate(currentSolarTerm, filter);
        String response = geminiClient.generateContent(prompt);
        log.info("LLM 선택 미션 생성 응답: {}", response);

        GeneratedSelectedMissionDto dto = parse(response);
        validate(dto);

        // 사용자가 지정한 태그는 요청값으로 확정하고, 미지정 태그만 LLM 응답값을 사용
        SpaceType spaceType = filter.getSpaceType() != null
                ? filter.getSpaceType()
                : parseEnumOrNull(SpaceType.class, dto.getSpaceType());
        CompanionType companionType = filter.getCompanionType() != null
                ? filter.getCompanionType()
                : parseEnumOrNull(CompanionType.class, dto.getCompanionType());
        CategoryType categoryType = filter.getCategoryType() != null
                ? filter.getCategoryType()
                : parseEnumOrNull(CategoryType.class, dto.getCategoryType());

        return LlmSelectedMissionResponse.of(
                dto.getTitle(),
                dto.getDescription(),
                spaceType,
                companionType,
                categoryType
        );
    }

    private GeneratedSelectedMissionDto parse(String response) {
        try {
            return objectMapper.readValue(response, GeneratedSelectedMissionDto.class);
        } catch (Exception e) {
            log.error("LLM 미션 응답 파싱 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.MISSION_GENERATION_FAILED);
        }
    }

    private void validate(GeneratedSelectedMissionDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            log.error("LLM 미션 응답에 title이 없습니다");
            throw new BusinessException(ErrorCode.MISSION_GENERATION_FAILED);
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            log.error("LLM 미션 응답에 description이 없습니다");
            throw new BusinessException(ErrorCode.MISSION_GENERATION_FAILED);
        }
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
}