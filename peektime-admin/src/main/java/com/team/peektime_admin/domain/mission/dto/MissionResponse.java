package com.team.peektime_admin.domain.mission.dto;

import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.global.common.enums.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MissionResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final SpaceType spaceType;
    private final IntensityType intensityType;
    private final CategoryType categoryType;
    private final CompanionType companionType;
    private final EnjoyType enjoyType;
    private final UserType userType;
    private final boolean deleted;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public MissionResponse(Mission mission) {
        this.id = mission.getId();
        this.title = mission.getTitle();
        this.description = mission.getDescription();
        this.spaceType = mission.getSpaceType();
        this.intensityType = mission.getIntensityType();
        this.categoryType = mission.getCategoryType();
        this.companionType = mission.getCompanionType();
        this.enjoyType = mission.getEnjoyType();
        this.userType = mission.getUserType();
        this.deleted = mission.isDeleted();
        this.createdAt = mission.getCreatedAt();
        this.updatedAt = mission.getUpdatedAt();
    }
}
