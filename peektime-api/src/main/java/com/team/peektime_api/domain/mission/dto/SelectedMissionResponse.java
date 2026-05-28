package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
import com.team.peektime_api.global.common.enums.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "선택 미션 응답")
@Getter
@Builder
public class SelectedMissionResponse {

    @Schema(description = "미션 ID")
    private Long id;

    @Schema(description = "미션 제목")
    private String title;

    @Schema(description = "미션 설명")
    private String description;

    @Schema(description = "공간 타입")
    private SpaceType spaceType;

    @Schema(description = "인원 타입")
    private CompanionType companionType;

    @Schema(description = "카테고리 타입")
    private CategoryType categoryType;

    public static SelectedMissionResponse from(Mission mission) {
        return SelectedMissionResponse.builder()
                .id(mission.getId())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .spaceType(mission.getSpaceType())
                .companionType(mission.getCompanionType())
                .categoryType(mission.getCategoryType())
                .build();
    }
}