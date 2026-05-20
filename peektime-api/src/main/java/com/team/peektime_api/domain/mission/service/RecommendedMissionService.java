package com.team.peektime_api.domain.mission.service;

import com.team.peektime_api.domain.mission.dto.RecommendedMissionResponse;
import com.team.peektime_api.domain.user.entity.UserOnboarding;
import com.team.peektime_api.domain.user.repository.UserOnboardingRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.infra.admin.dto.AdminRecommendedMissionResponse;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendedMissionService {

    private final UserOnboardingRepository userOnboardingRepository;
    private final AdminClient adminClient;

    public RecommendedMissionResponse getRecommendedMissions(Long userId) {
        UserOnboarding onboarding = userOnboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        AdminRecommendedMissionResponse adminResponse = adminClient.getRecommendedMissions(
                onboarding.getUserType().name(),
                onboarding.getEnjoyTypeFirst().name(),
                onboarding.getEnjoyTypeSecond().name(),
                onboarding.getEnjoyTypeThird().name()
        );

        return RecommendedMissionResponse.from(adminResponse);
    }
}
