package com.team.peektime_admin.domain.mission.service;

import com.team.peektime_admin.domain.mission.dto.GeneratedMissionDto;
import com.team.peektime_admin.domain.mission.dto.GeneratedMissionsWrapper;
import com.team.peektime_admin.domain.mission.dto.MissionGenerationResult;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.prompt.MissionPromptTemplate;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.domain.sync.service.SyncService;
import com.team.peektime_admin.global.common.enums.*;
import com.team.peektime_admin.infra.llm.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionGenerationService {

    private final GeminiClient geminiClient;
    private final JsonMapper jsonMapper;
    private final SolarTermRepository solarTermRepository;
    private final MissionRepository missionRepository;
    private final SyncService syncService;

    /**
     * 절기가 주어지면 enjoyType(자연/야외, 제철음식, 감성콘텐츠) 3종을 각각 단일 역할로 병렬 호출하여
     * 생성한다. 한 번의 호출에 모든 enjoyType을 섞어 요구하는 대신 역할을 좁혀 본문 품질과 태그
     * 정확도를 높인다. enjoyType 태그는 LLM 응답 대신 요청값으로 확정한다.
     */
    @Transactional
    public MissionGenerationResult generateMissionsWithSolarTerm(Long solarTermId, int count) {
        SolarTerm solarTerm = solarTermRepository.findById(solarTermId)
                .orElseThrow(() -> new IllegalArgumentException("절기를 찾을 수 없습니다: " + solarTermId));

        EnjoyType[] enjoyTypes = EnjoyType.values();
        int[] counts = distribute(count, enjoyTypes.length);

        List<GeneratedMissionDto> missions = callPerEnjoyTypeInParallel(solarTerm, enjoyTypes, counts);
        return saveMissionsAndSync(missions);
    }

    private List<GeneratedMissionDto> callPerEnjoyTypeInParallel(
            SolarTerm solarTerm, EnjoyType[] enjoyTypes, int[] counts) {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<List<GeneratedMissionDto>>> futures = new ArrayList<>();

            for (int i = 0; i < enjoyTypes.length; i++) {
                EnjoyType enjoyType = enjoyTypes[i];
                int subCount = counts[i];
                if (subCount <= 0) {
                    continue;
                }

                String prompt = MissionPromptTemplate.generateWithSolarTermAndEnjoyType(
                        solarTerm, enjoyType, subCount);

                futures.add(CompletableFuture
                        .supplyAsync(() -> callGeminiAndParse(prompt), executor)
                        .thenApply(dtos -> {
                            dtos.forEach(dto -> dto.overrideEnjoyType(enjoyType));
                            return dtos;
                        })
                        .exceptionally(e -> {
                            log.error("enjoyType={} 미션 생성 실패: {}", enjoyType, e.getMessage());
                            return List.of();
                        }));
            }

            return futures.stream()
                    .flatMap(future -> future.join().stream())
                    .toList();
        }
    }

    /**
     * total 개수를 size 등분한다. 나누어떨어지지 않는 나머지는 앞쪽 그룹부터 1개씩 더 배분한다.
     * 예) distribute(10, 3) -> [4, 3, 3]
     */
    private int[] distribute(int total, int size) {
        int[] result = new int[size];
        int base = total / size;
        int remainder = total % size;
        for (int i = 0; i < size; i++) {
            result[i] = base + (i < remainder ? 1 : 0);
        }
        return result;
    }

    private MissionGenerationResult saveMissionsAndSync(List<GeneratedMissionDto> missionDtos) {
        List<Mission> missions = new ArrayList<>();
        List<GeneratedMissionDto> savedMissionDtos = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();

        for (int i = 0; i < missionDtos.size(); i++) {
            GeneratedMissionDto dto = missionDtos.get(i);
            try {
                missions.add(toEntity(dto));
                savedMissionDtos.add(dto);
            } catch (IllegalArgumentException e) {
                String title = dto.getTitle() != null ? dto.getTitle() : "(제목 없음)";
                String reason = (i + 1) + "번째 미션 제외: " + title + " - " + e.getMessage();
                skippedReasons.add(reason);
                log.warn(reason);
            }
        }

        if (missions.isEmpty()) {
            log.warn("저장 가능한 미션이 없습니다. 제외 사유: {}", skippedReasons);
            return MissionGenerationResult.allSkipped(skippedReasons);
        }

        missionRepository.saveAll(missions);
        log.info("{}개의 미션이 저장되었습니다. 제외된 미션: {}개", missions.size(), skippedReasons.size());

        // API 서버로 자동 동기화
        try {
            syncService.syncAllMissions();
            log.info("API 서버로 미션 동기화 완료");
            return MissionGenerationResult.success(savedMissionDtos, skippedReasons);
        } catch (Exception e) {
            log.warn("API 서버 동기화 실패 (미션 저장은 완료됨): {}", e.getMessage());
            return MissionGenerationResult.syncFailed(savedMissionDtos, skippedReasons, e.getMessage());
        }
    }

    private Mission toEntity(GeneratedMissionDto dto) {
        return Mission.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .spaceType(parseEnum(SpaceType.class, dto.getSpaceType()))
                .companionType(parseEnum(CompanionType.class, dto.getCompanionType()))
                .categoryType(parseEnum(CategoryType.class, dto.getCategoryType()))
                .enjoyType(parseEnumOrNull(EnjoyType.class, dto.getEnjoyType()))
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
