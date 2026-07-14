package com.team.peektime_api.domain.mission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.dto.GeneratedSelectedMissionDto;
import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.prompt.LlmSelectedMissionPromptTemplate;
import com.team.peektime_api.domain.mission.repository.UserSelectedMissionRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.global.aop.DistributedLock;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.llm.GeminiClient;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * LLM 선택 미션 생성 플로우 (조회 → 프롬프트 → LLM 호출 → 저장).
 * 사용자별 분산 락으로 따닥(동시 중복 요청)의 두 번째 요청이
 * LLM 호출 비용을 태우기 전에 즉시 실패하도록 막는다.
 * 락은 최선의 방어일 뿐, 중복 저장의 최종 보장은 (user_id, selected_date) unique 제약이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmSelectedMissionGenerator {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final SolarTermRepository solarTermRepository;
    private final UserSelectedMissionRepository userSelectedMissionRepository;
    private final LlmMissionRegistrar llmMissionRegistrar;

    // waitTime 0: 따닥의 두 번째 요청은 대기 없이 즉시 409(LOCK_ACQUISITION_FAILED)
    // leaseTime 40s: async servlet 타임아웃과 동일. 서버가 죽어도 이 시간이 지나면 자동 해제
    // transactional false: LLM 호출(최대 30초) 동안 DB 커넥션을 점유하지 않도록 트랜잭션 없이 락만 건다.
    //                      저장은 LlmMissionRegistrar의 트랜잭션에서 처리한다
    @DistributedLock(key = "'llm-selected-mission:' + #userId", waitTime = 0L, leaseTime = 40L, transactional = false)
    public Mission generate(Long userId, SelectedMissionRequest filter) {
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

        return registerMission(userId, currentSolarTerm.getId(), today, dto, filter);
    }

    // 태그는 요청에서 모두 필수값으로 검증되므로 요청값을 그대로 사용
    private Mission registerMission(Long userId, Long solarTermId, LocalDate today,
                                    GeneratedSelectedMissionDto dto, SelectedMissionRequest filter) {
        try {
            return llmMissionRegistrar.register(
                    userId, solarTermId, today,
                    dto.getTitle(), dto.getDescription(),
                    filter.getSpaceType(), filter.getCompanionType(), filter.getCategoryType()
            );
        } catch (DataIntegrityViolationException e) {
            // 락 밖의 경로가 LLM 호출 중에 먼저 선택을 저장한 경우:
            // 락 lease(40s) 만료 후 재시도 등.
            // (user_id, selected_date) unique 제약 위반으로 감지해 409로 응답
            throw new BusinessException(ErrorCode.MISSION_ALREADY_SELECTED);
        }
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
}