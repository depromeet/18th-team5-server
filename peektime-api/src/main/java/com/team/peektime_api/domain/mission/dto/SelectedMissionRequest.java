package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
import com.team.peektime_api.global.common.enums.IntensityType;
import com.team.peektime_api.global.common.enums.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "선택 미션 필터 요청")
@Getter
@Setter
public class SelectedMissionRequest {

    @Schema(description = "공간 필터 (실내/실외)", example = "INDOOR")
    private SpaceType spaceType;

    @Schema(description = "이동거리 필터 (가벼운/보통/적극적)", example = "LIGHT")
    private IntensityType intensityType;

    @Schema(description = "인원 필터 (혼자/같이)", example = "TOGETHER")
    private CompanionType companionType;

    @Schema(description = "카테고리 필터 (음식/자연/기록/장소/감각)", example = "FOOD")
    private CategoryType categoryType;
}