package com.team.peektime_admin.domain.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MissionGenerationResult {

    private List<GeneratedMissionDto> missions;
    private boolean syncSuccess;
    private String syncMessage;

    public static MissionGenerationResult success(List<GeneratedMissionDto> missions) {
        return MissionGenerationResult.builder()
                .missions(missions)
                .syncSuccess(true)
                .syncMessage("API 서버 동기화 완료")
                .build();
    }

    public static MissionGenerationResult syncFailed(List<GeneratedMissionDto> missions, String errorMessage) {
        return MissionGenerationResult.builder()
                .missions(missions)
                .syncSuccess(false)
                .syncMessage("API 서버 동기화 실패: " + errorMessage)
                .build();
    }
}
