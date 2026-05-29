package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
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

    @Schema(description = "인원 필터 (혼자/같이)", example = "TOGETHER")
    private CompanionType companionType;

    @Schema(description = "카테고리 필터 (음식/자연/컨텐츠/장소/음악)", example = "MUSIC")
    private CategoryType categoryType;
}