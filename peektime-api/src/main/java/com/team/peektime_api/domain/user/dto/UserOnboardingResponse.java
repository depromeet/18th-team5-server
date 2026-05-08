package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.domain.user.entity.UserOnboarding;
import com.team.peektime_api.global.common.enums.UserType;
import lombok.Getter;

@Getter
public class UserOnboardingResponse {

    private final UserType userType;
    private final String userTypeLabel;

    public UserOnboardingResponse(UserOnboarding onboarding) {
        this.userType = onboarding.getUserType();
        this.userTypeLabel = onboarding.getUserType().getLabel();
    }
}
