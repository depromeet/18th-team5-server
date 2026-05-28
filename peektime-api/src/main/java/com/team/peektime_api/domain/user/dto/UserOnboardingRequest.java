package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.common.enums.ActivityStyleType;
import com.team.peektime_api.global.common.enums.SpaceType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserOnboardingRequest {

    @NotNull
    private SpaceType spaceType;

    @NotNull
    private ActivityStyleType activityStyleType;

    @NotNull
    private EnjoyType enjoyTypeFirst;

    @NotNull
    private EnjoyType enjoyTypeSecond;

    @NotNull
    private EnjoyType enjoyTypeThird;
}
