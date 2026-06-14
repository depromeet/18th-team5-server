package com.team.peektime_admin.domain.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MissionGenerationResponse {

    private int generatedCount;
    private List<GeneratedMissionDto> missions;
    private int skippedCount;
    private List<String> skippedReasons;
    private boolean syncSuccess;
    private String syncMessage;

    public static MissionGenerationResponse of(List<GeneratedMissionDto> missions, boolean syncSuccess, String syncMessage) {
        return MissionGenerationResponse.builder()
                .generatedCount(missions.size())
                .missions(missions)
                .skippedCount(0)
                .skippedReasons(List.of())
                .syncSuccess(syncSuccess)
                .syncMessage(syncMessage)
                .build();
    }

    public static MissionGenerationResponse of(MissionGenerationResult result) {
        List<String> skippedReasons = result.getSkippedReasons() != null ? result.getSkippedReasons() : List.of();
        return MissionGenerationResponse.builder()
                .generatedCount(result.getMissions().size())
                .missions(result.getMissions())
                .skippedCount(skippedReasons.size())
                .skippedReasons(skippedReasons)
                .syncSuccess(result.isSyncSuccess())
                .syncMessage(result.getSyncMessage())
                .build();
    }
}
