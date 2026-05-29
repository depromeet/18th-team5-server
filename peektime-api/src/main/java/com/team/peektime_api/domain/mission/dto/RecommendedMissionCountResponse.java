package com.team.peektime_api.domain.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "추천 미션 완료 횟수 응답")
@Getter
@Builder
public class RecommendedMissionCountResponse {

    @Schema(description = "추천 미션 완료 횟수", example = "5")
    private long completedCount;

    public static RecommendedMissionCountResponse of(long count) {
        return RecommendedMissionCountResponse.builder()
                .completedCount(count)
                .build();
    }
}