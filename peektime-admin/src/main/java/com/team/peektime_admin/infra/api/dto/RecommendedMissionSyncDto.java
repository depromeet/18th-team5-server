package com.team.peektime_admin.infra.api.dto;

import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.global.common.enums.UserType;

public record RecommendedMissionSyncDto(
        Long id,
        Long missionId,
        Long solarTermId,
        UserType userType,
        Integer displayOrder
) {
    public static RecommendedMissionSyncDto from(RecommendedMissionPool recommendedMission) {
        return new RecommendedMissionSyncDto(
                recommendedMission.getId(),
                recommendedMission.getMission().getId(),
                recommendedMission.getSolarTerm().getId(),
                recommendedMission.getUserType(),
                recommendedMission.getDisplayOrder()
        );
    }
}