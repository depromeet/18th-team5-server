package com.team.peektime_api.domain.mission.service;

import com.team.peektime_api.domain.mission.dto.RecommendedMissionResponse;
import com.team.peektime_api.domain.mission.dto.RecommendedMissionResponse.MissionItem;
import com.team.peektime_api.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_api.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.user.entity.UserOnboarding;
import com.team.peektime_api.domain.user.repository.UserOnboardingRepository;
import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendedMissionService {

    private final UserOnboardingRepository userOnboardingRepository;
    private final SolarTermRepository solarTermRepository;
    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;
    private final UserMissionCompletionRepository userMissionCompletionRepository;

    public RecommendedMissionResponse getRecommendedMissions(Long userId) {
        UserOnboarding onboarding = userOnboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        SolarTerm currentSolarTerm = solarTermRepository.findByDate(LocalDate.now())
                .orElse(null);

        if (currentSolarTerm == null) {
            return RecommendedMissionResponse.of(onboarding.getUserType(), null, List.of());
        }

        List<RecommendedMissionPool> pools = recommendedMissionPoolRepository
                .findBySolarTermIdAndUserTypeOrderByDisplayOrderAsc(currentSolarTerm.getId(), onboarding.getUserType());

        List<EnjoyType> priority = List.of(
                onboarding.getEnjoyTypeFirst(),
                onboarding.getEnjoyTypeSecond(),
                onboarding.getEnjoyTypeThird()
        );

        List<MissionItem> missions = sortByPriority(pools, priority, userId);

        return RecommendedMissionResponse.of(onboarding.getUserType(), currentSolarTerm, missions);
    }

    private List<MissionItem> sortByPriority(List<RecommendedMissionPool> pools, List<EnjoyType> priority, Long userId) {
        Map<EnjoyType, List<RecommendedMissionPool>> grouped = new LinkedHashMap<>();
        for (RecommendedMissionPool pool : pools) {
            grouped.computeIfAbsent(pool.getMission().getEnjoyType(), k -> new ArrayList<>()).add(pool);
        }

        List<MissionItem> result = new ArrayList<>();
        int order = 1;

        for (EnjoyType enjoyType : priority) {
            for (RecommendedMissionPool pool : grouped.getOrDefault(enjoyType, List.of())) {
                boolean isCompleted = userMissionCompletionRepository
                        .existsByUser_IdAndMission_Id(userId, pool.getMission().getId());
                result.add(MissionItem.from(pool, order++, isCompleted));
            }
        }

        return result;
    }
}
