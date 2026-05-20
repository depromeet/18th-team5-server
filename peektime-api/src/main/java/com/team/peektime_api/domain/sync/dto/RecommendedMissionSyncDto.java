package com.team.peektime_api.domain.sync.dto;

import com.team.peektime_api.global.common.enums.UserType;

public record RecommendedMissionSyncDto(
        Long id,
        Long missionId,
        Long solarTermId,
        UserType userType,
        Integer displayOrder
) {
}