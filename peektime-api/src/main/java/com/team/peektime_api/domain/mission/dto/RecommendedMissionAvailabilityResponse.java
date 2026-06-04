package com.team.peektime_api.domain.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "추천 미션 가능 여부 응답")
@Getter
@Builder
public class RecommendedMissionAvailabilityResponse {

    private static final int MAX_DAILY_COUNT = 3;

    @Schema(description = "추천 미션 완료 가능 여부", example = "true")
    private Boolean available;

    @Schema(description = "오늘 완료한 추천 미션 수", example = "1")
    private Integer completedCount;

    @Schema(description = "하루 최대 완료 가능 수", example = "3")
    private Integer maxCount;

    @Schema(description = "남은 완료 가능 수", example = "2")
    private Integer remainingCount;

    public static RecommendedMissionAvailabilityResponse of(int completedCount) {
        int remaining = Math.max(0, MAX_DAILY_COUNT - completedCount);
        return RecommendedMissionAvailabilityResponse.builder()
                .available(completedCount < MAX_DAILY_COUNT)
                .completedCount(completedCount)
                .maxCount(MAX_DAILY_COUNT)
                .remainingCount(remaining)
                .build();
    }
}