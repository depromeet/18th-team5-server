package com.team.peektime_api.domain.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "오늘 선택 미션 조회 여부 응답")
@Getter
@Builder
public class SelectedMissionStatusResponse {

    @Schema(description = "오늘 선택 미션을 조회한 적이 있는지 여부", example = "true")
    private boolean hasSelectedToday;

    public static SelectedMissionStatusResponse of(boolean hasSelectedToday) {
        return SelectedMissionStatusResponse.builder()
                .hasSelectedToday(hasSelectedToday)
                .build();
    }
}