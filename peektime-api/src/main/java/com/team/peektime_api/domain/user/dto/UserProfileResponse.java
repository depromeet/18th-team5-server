package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.domain.user.entity.User;
import lombok.Getter;

@Getter
public class UserProfileResponse {

    private final Long userId;
    private final boolean onboardingCompleted;

    public UserProfileResponse(User user) {
        this.userId = user.getId();
        this.onboardingCompleted = user.getOnboarding() != null;
    }
}
