package com.team.peektime_api.domain.mission.service;

import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.dto.SelectedMissionResponse;
import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.repository.MissionRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.user.entity.UserOnboarding;
import com.team.peektime_api.domain.user.repository.UserOnboardingRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SelectedMissionService {

    private final MissionRepository missionRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final SolarTermRepository solarTermRepository;

    public List<SelectedMissionResponse> getSelectedMissions(Long userId, SelectedMissionRequest filter) {
        UserOnboarding onboarding = userOnboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        SolarTerm currentSolarTerm = solarTermRepository.findByDate(LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));

        List<Mission> missions = missionRepository.findSelectedMissions(
                currentSolarTerm.getId(),
                onboarding.getUserType(),
                filter
        );

        return missions.stream()
                .map(SelectedMissionResponse::from)
                .toList();
    }
}