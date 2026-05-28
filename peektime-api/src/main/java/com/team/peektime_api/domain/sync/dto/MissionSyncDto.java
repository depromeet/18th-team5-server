package com.team.peektime_api.domain.sync.dto;

import com.team.peektime_api.global.common.enums.*;

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
}