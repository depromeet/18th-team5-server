package com.team.peektime_admin.infra.api.dto;

import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.global.common.enums.*;

public record MissionSyncDto(
        Long id,
        String title,
        String description,
        SpaceType spaceType,
        CategoryType categoryType,
        CompanionType companionType,
        EnjoyType enjoyType,
        UserType userType,
        boolean deleted
) {
    public static MissionSyncDto from(Mission mission) {
        return new MissionSyncDto(
                mission.getId(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getSpaceType(),
                mission.getCategoryType(),
                mission.getCompanionType(),
                mission.getEnjoyType(),
                mission.getUserType(),
                mission.isDeleted()
        );
    }
}