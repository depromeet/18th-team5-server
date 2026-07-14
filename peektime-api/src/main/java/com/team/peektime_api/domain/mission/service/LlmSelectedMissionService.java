package com.team.peektime_api.domain.mission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.dto.GeneratedSelectedMissionDto;
import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.dto.SelectedMissionResponse;
import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.prompt.LlmSelectedMissionPromptTemplate;
import com.team.peektime_api.domain.mission.repository.UserSelectedMissionRepository;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

// LLM 호출(최대 30초) 동안 DB 커넥션을 점유하지 않도록 트랜잭션을 걸지 않고,
// 저장은 LlmMissionRegistrar의 트랜잭션에서 처리한다
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmSelectedMissionService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final SolarTermRepository solarTermRepository;
    private final UserSelectedMissionRepository userSelectedMissionRepository;
    private final LlmMissionRegistrar llmMissionRegistrar;

    // @Async 프록시가 메서드 전체를 llmExecutor(요청당 가상 스레드)에서 실행하고
    // CompletableFuture 반환으로 톰캣 워커는 즉시 반납된다.
    // 메서드가 리턴하는 시점엔 결과가 이미 있으므로 completedFuture로 감싼다
    @Async("llmExecutor")
    public CompletableFuture<SelectedMissionResponse> generateSelectedMission(Long userId, SelectedMissionRequest filter) {
        LocalDate today = LocalDate.now();

        // 이 API는 오늘 첫 미션 선택인 사용자만 호출한다는 전제.
        // 이미 선택한 미션은 조회 API(/selected/today)로 제공하므로 여기서는 LLM 호출 전에 409로 차단한다
        if (userSelectedMissionRepository.findByUserIdAndSelectedDate(userId, today).isPresent()) {
            throw new BusinessException(ErrorCode.MISSION_ALREADY_SELECTED);
        }

        SolarTerm currentSolarTerm = solarTermRepository.findByDate(today)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));

        String prompt = LlmSelectedMissionPromptTemplate.generate(currentSolarTerm, filter);
        String response = geminiClient.generateContent(prompt);
        log.info("LLM 선택 미션 생성 응답: {}", response);

        GeneratedSelectedMissionDto dto = parse(response);
        validate(dto);

        // 사용자가 지정한 태그는 요청값으로 확정하고, 미지정 태그만 LLM 응답값을 사용
        SpaceType spaceType = resolveTag(SpaceType.class, filter.getSpaceType(), dto.getSpaceType());
        CompanionType companionType = resolveTag(CompanionType.class, filter.getCompanionType(), dto.getCompanionType());
        CategoryType categoryType = resolveTag(CategoryType.class, filter.getCategoryType(), dto.getCategoryType());

        Mission mission = llmMissionRegistrar.register(
                userId, currentSolarTerm.getId(), today,
                dto.getTitle(), dto.getDescription(),
                spaceType, companionType, categoryType
        );

        return CompletableFuture.completedFuture(SelectedMissionResponse.from(mission));
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

    // Mission 태그 컬럼은 NOT NULL이므로 요청값도 LLM 응답값도 없으면 생성 실패로 처리
    private <T extends Enum<T>> T resolveTag(Class<T> enumClass, T requested, String generated) {
        if (requested != null) {
            return requested;
        }
        if (generated == null || generated.isBlank()) {
            log.error("LLM 미션 응답에 {} 값이 없습니다", enumClass.getSimpleName());
            throw new BusinessException(ErrorCode.MISSION_GENERATION_FAILED);
        }
        try {
            return Enum.valueOf(enumClass, generated.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("알 수 없는 {} 값: {}", enumClass.getSimpleName(), generated);
            throw new BusinessException(ErrorCode.MISSION_GENERATION_FAILED);
        }
    }
}