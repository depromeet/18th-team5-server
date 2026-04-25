package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.domain.user.entity.UserOnboarding;
import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.common.enums.IntensityType;
import com.team.peektime_api.global.common.enums.SpaceType;
import com.team.peektime_api.global.common.enums.UserType;
import lombok.Getter;

@Getter
public class UserOnboardingResponse {

    private final Long userId;
    private final SpaceType spaceType;
    private final IntensityType intensityType;
    private final EnjoyType enjoyTypeFirst;
    private final EnjoyType enjoyTypeSecond;
    private final EnjoyType enjoyTypeThird;
    private final UserType userType;

    public UserOnboardingResponse(UserOnboarding onboarding) {
        this.userId = onboarding.getUser().getId();
        this.spaceType = onboarding.getSpaceType();
        this.intensityType = onboarding.getIntensityType();
        this.enjoyTypeFirst = onboarding.getEnjoyTypeFirst();
        this.enjoyTypeSecond = onboarding.getEnjoyTypeSecond();
        this.enjoyTypeThird = onboarding.getEnjoyTypeThird();
        this.userType = onboarding.getUserType();
    }
}
