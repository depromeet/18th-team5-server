package com.team.peektime_api.global.infra.admin.dto;

import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.EnjoyType;

import java.util.List;

public record AdminRecommendedMissionResponse(
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
}
