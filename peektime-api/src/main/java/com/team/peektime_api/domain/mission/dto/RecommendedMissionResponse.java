package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.common.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "추천 미션 응답")
public record RecommendedMissionResponse(
        @Schema(description = "사용자 타입", example = "EXPLORER")
        UserType userType,

        @Schema(description = "현재 절기 이름", example = "소만")
        String solarTerm,

        @Schema(description = "추천 미션 목록")
        List<MissionItem> missions
) {

    public static RecommendedMissionResponse of(UserType userType, SolarTerm solarTerm, List<MissionItem> missions) {
        return new RecommendedMissionResponse(
                userType,
                solarTerm != null ? solarTerm.getName() : null,
                missions
        );
    }

    @Schema(description = "미션 항목")
    public record MissionItem(
            @Schema(description = "미션 ID")
            Long id,

            @Schema(description = "미션 제목")
            String title,

            @Schema(description = "미션 설명")
            String description,

            @Schema(description = "카테고리 타입")
            CategoryType categoryType,

            @Schema(description = "즐기기 타입")
            EnjoyType enjoyType,

            @Schema(description = "정렬 순서")
            int order,

            @Schema(description = "완료 여부")
            boolean isCompleted
    ) {
        public static MissionItem from(RecommendedMissionPool pool, int order, boolean isCompleted) {
            return new MissionItem(
                    pool.getMission().getId(),
                    pool.getMission().getTitle(),
                    pool.getMission().getDescription(),
                    pool.getMission().getCategoryType(),
                    pool.getMission().getEnjoyType(),
                    order,
                    isCompleted
            );
        }
    }
}