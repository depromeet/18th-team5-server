package com.team.peektime_api.domain.user.service;

import com.team.peektime_api.domain.user.dto.UserOnboardingRequest;
import com.team.peektime_api.domain.user.dto.UserOnboardingResponse;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.entity.UserCategoryPreference;
import com.team.peektime_api.domain.user.repository.UserCategoryPreferenceRepository;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.common.enums.ActivityStyle;
import com.team.peektime_api.global.common.enums.SpaceType;
import com.team.peektime_api.global.common.enums.UserType;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserCategoryPreferenceRepository categoryPreferenceRepository;

    public UserOnboardingResponse saveOnboarding(Long userId, UserOnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserType userType = determineUserType(request.getActivityLocation(), request.getActivityStyle());
        user.updateUserType(userType);

        categoryPreferenceRepository.deleteByUser(user);

        List<UserCategoryPreference> preferences = request.getCategoryRanks().stream()
                .map(item -> UserCategoryPreference.builder()
                        .user(user)
                        .category(item.getCategory())
                        .rank(item.getRank())
                        .build())
                .toList();
        categoryPreferenceRepository.saveAll(preferences);

        return new UserOnboardingResponse(user, preferences);
    }

    private UserType determineUserType(SpaceType location, ActivityStyle style) {
        if (location == SpaceType.OUTDOOR && style == ActivityStyle.ACTIVE) return UserType.EXPLORER;
        if (location == SpaceType.OUTDOOR && style == ActivityStyle.CASUAL) return UserType.WALKER;
        if (location == SpaceType.INDOOR && style == ActivityStyle.ACTIVE) return UserType.LIFE_CREATOR;
        return UserType.AESTHETE;
    }
}
