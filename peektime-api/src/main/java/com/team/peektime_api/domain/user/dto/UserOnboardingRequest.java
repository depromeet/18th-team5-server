package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.common.enums.IntensityType;
import com.team.peektime_api.global.common.enums.SpaceType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UserOnboardingRequest {

    @NotNull
    private SpaceType spaceType;

    @NotNull
    private IntensityType intensityType;

    @NotNull
    private EnjoyType enjoyTypeFirst;

    @NotNull
    private EnjoyType enjoyTypeSecond;

    @NotNull
    private EnjoyType enjoyTypeThird;
}
