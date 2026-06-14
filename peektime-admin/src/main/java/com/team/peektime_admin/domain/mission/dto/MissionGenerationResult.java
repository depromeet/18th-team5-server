package com.team.peektime_admin.domain.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MissionGenerationResult {

    private List<GeneratedMissionDto> missions;
    private List<String> skippedReasons;
    private boolean syncSuccess;
    private String syncMessage;

    public static MissionGenerationResult success(List<GeneratedMissionDto> missions) {
        return MissionGenerationResult.builder()
                .missions(missions)
                .skippedReasons(List.of())
                .syncSuccess(true)
                .syncMessage("API 서버 동기화 완료")
                .build();
    }

    public static MissionGenerationResult success(List<GeneratedMissionDto> missions, List<String> skippedReasons) {
        return MissionGenerationResult.builder()
                .missions(missions)
                .skippedReasons(skippedReasons)
                .syncSuccess(true)
                .syncMessage("API 서버 동기화 완료")
                .build();
    }

    public static MissionGenerationResult syncFailed(List<GeneratedMissionDto> missions, List<String> skippedReasons, String errorMessage) {
        return MissionGenerationResult.builder()
                .missions(missions)
                .skippedReasons(skippedReasons)
                .syncSuccess(false)
                .syncMessage("API 서버 동기화 실패: " + errorMessage)
                .build();
    }

    public static MissionGenerationResult allSkipped(List<String> skippedReasons) {
        return MissionGenerationResult.builder()
                .missions(List.of())
                .skippedReasons(skippedReasons)
                .syncSuccess(false)
                .syncMessage("저장 가능한 미션이 없어 API 서버 동기화를 건너뛰었습니다.")
                .build();
    }
}
