package com.team.peektime_api.domain.user.service;

import com.team.peektime_api.domain.user.dto.UserOnboardingRequest;
import com.team.peektime_api.domain.user.dto.UserOnboardingResponse;
import com.team.peektime_api.domain.user.dto.UserProfileResponse;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.entity.UserOnboarding;
import com.team.peektime_api.domain.user.repository.UserOnboardingRepository;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserOnboardingRepository userOnboardingRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponse.from(user);
    }

    public UserOnboardingResponse saveOnboarding(Long userId, UserOnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserOnboarding onboarding = userOnboardingRepository.findByUserId(userId)
                .map(existing -> {
                    existing.update(request.getSpaceType(), request.getIntensityType(),
                            request.getEnjoyTypeFirst(), request.getEnjoyTypeSecond(), request.getEnjoyTypeThird());
                    return existing;
                })
                .orElseGet(() -> userOnboardingRepository.save(UserOnboarding.builder()
                        .user(user)
                        .spaceType(request.getSpaceType())
                        .intensityType(request.getIntensityType())
                        .enjoyTypeFirst(request.getEnjoyTypeFirst())
                        .enjoyTypeSecond(request.getEnjoyTypeSecond())
                        .enjoyTypeThird(request.getEnjoyTypeThird())
                        .build()));

        return new UserOnboardingResponse(onboarding);
    }
}
