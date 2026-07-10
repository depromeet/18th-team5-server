package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
import com.team.peektime_api.global.common.enums.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "LLM 생성 선택 미션 응답")
@Getter
@Builder
public class LlmSelectedMissionResponse {

    @Schema(description = "미션 제목", example = "과일가게 붉은 살구 구경하기")
    private String title;

    @Schema(description = "미션 설명", example = "출근길 과일가게에서 여름빛 살구를 찾아보세요")
    private String description;

    @Schema(description = "공간 타입")
    private SpaceType spaceType;

    @Schema(description = "인원 타입")
    private CompanionType companionType;

    @Schema(description = "카테고리 타입")
    private CategoryType categoryType;

    public static LlmSelectedMissionResponse of(
            String title,
            String description,
            SpaceType spaceType,
            CompanionType companionType,
            CategoryType categoryType
    ) {
        return LlmSelectedMissionResponse.builder()
                .title(title)
                .description(description)
                .spaceType(spaceType)
                .companionType(companionType)
                .categoryType(categoryType)
                .build();
    }
}