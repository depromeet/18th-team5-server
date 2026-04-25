package com.team.peektime_admin.domain.mission.dto;

import com.team.peektime_admin.global.common.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MissionRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private SpaceType spaceType;

    @NotNull
    private IntensityType intensityType;

    @NotNull
    private CategoryType categoryType;

    @NotNull
    private CompanionType companionType;

    private EnjoyType enjoyType;

    private UserType userType;
}
