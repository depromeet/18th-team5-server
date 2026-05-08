package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.enums.UserType;

public record UserProfileResponse(
        Long userId,
        String nickname,
        boolean onboardingCompleted,
        UserType userType
) {
    public static UserProfileResponse from(User user) {
        boolean hasOnboarding = user.getOnboarding() != null;
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                hasOnboarding,
                hasOnboarding ? user.getOnboarding().getUserType() : null
        );
    }
}
