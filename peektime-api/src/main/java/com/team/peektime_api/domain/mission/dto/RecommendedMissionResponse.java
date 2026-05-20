package com.team.peektime_api.domain.mission.dto;

import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.infra.admin.dto.AdminRecommendedMissionResponse;

import java.util.List;

public record RecommendedMissionResponse(
        HeaderInfo header,
        List<MissionItem> missions
) {

    public record HeaderInfo(
            String title,
            String subtitle
    ) {}

    public record MissionItem(
            Long id,
            String title,
            CategoryType categoryType,
            EnjoyType enjoyType,
            int order
    ) {}

    public static RecommendedMissionResponse from(AdminRecommendedMissionResponse adminResponse) {
        HeaderInfo header = new HeaderInfo(
                adminResponse.header().title(),
                adminResponse.header().subtitle()
        );
        List<MissionItem> missions = adminResponse.missions().stream()
                .map(m -> new MissionItem(m.id(), m.title(), m.categoryType(), m.enjoyType(), m.order()))
                .toList();
        return new RecommendedMissionResponse(header, missions);
    }
}
