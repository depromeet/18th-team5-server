package com.team.peektime_admin.domain.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MissionGenerationResponse {

    private int generatedCount;
    private List<GeneratedMissionDto> missions;
    private boolean syncSuccess;
    private String syncMessage;

    public static MissionGenerationResponse of(List<GeneratedMissionDto> missions, boolean syncSuccess, String syncMessage) {
        return MissionGenerationResponse.builder()
                .generatedCount(missions.size())
                .missions(missions)
                .syncSuccess(syncSuccess)
                .syncMessage(syncMessage)
                .build();
    }
}