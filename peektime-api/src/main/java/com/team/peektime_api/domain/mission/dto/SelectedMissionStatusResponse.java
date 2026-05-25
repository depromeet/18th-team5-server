package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.Mission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "오늘 선택 미션 조회 응답")
@Getter
@Builder
public class SelectedMissionStatusResponse {

    @Schema(description = "오늘 선택 미션을 조회한 적이 있는지 여부", example = "true")
    private boolean hasSelected;

    @Schema(description = "선택한 미션 정보 (선택한 적 없으면 null)")
    private SelectedMissionResponse mission;

    public static SelectedMissionStatusResponse of(boolean hasSelected, Mission mission) {
        return SelectedMissionStatusResponse.builder()
                .hasSelected(hasSelected)
                .mission(mission != null ? SelectedMissionResponse.from(mission) : null)
                .build();
    }
}