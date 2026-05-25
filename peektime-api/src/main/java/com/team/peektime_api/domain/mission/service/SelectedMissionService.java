package com.team.peektime_api.domain.mission.service;

import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.dto.SelectedMissionResponse;
import com.team.peektime_api.domain.mission.dto.SelectedMissionStatusResponse;
import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.entity.UserSelectedMission;
import com.team.peektime_api.domain.mission.repository.MissionRepository;
import com.team.peektime_api.domain.mission.repository.UserSelectedMissionRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.entity.UserOnboarding;
import com.team.peektime_api.domain.user.repository.UserOnboardingRepository;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SelectedMissionService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final UserSelectedMissionRepository userSelectedMissionRepository;
    private final SolarTermRepository solarTermRepository;

    public SelectedMissionStatusResponse getTodaySelectedStatus(Long userId) {
        LocalDate today = LocalDate.now();
        boolean hasSelected = userSelectedMissionRepository.existsByUserIdAndSelectedDate(userId, today);
        return SelectedMissionStatusResponse.of(hasSelected);
    }

    @Transactional
    public SelectedMissionResponse getSelectedMission(Long userId, SelectedMissionRequest filter) {
        LocalDate today = LocalDate.now();

        // 오늘 이미 선택한 미션이 있으면 반환
        return userSelectedMissionRepository.findByUserIdAndSelectedDate(userId, today)
                .map(selected -> SelectedMissionResponse.from(selected.getMission()))
                .orElseGet(() -> selectNewMission(userId, filter, today));
    }

    private SelectedMissionResponse selectNewMission(Long userId, SelectedMissionRequest filter, LocalDate today) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserOnboarding onboarding = userOnboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        SolarTerm currentSolarTerm = solarTermRepository.findByDate(today)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));

        List<Mission> missions = missionRepository.findSelectedMissions(
                userId,
                currentSolarTerm.getId(),
                onboarding.getUserType(),
                filter
        );

        if (missions.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSION_NOT_FOUND);
        }

        // 랜덤으로 1개 선택
        Mission selectedMission = missions.get(ThreadLocalRandom.current().nextInt(missions.size()));

        // 저장
        UserSelectedMission userSelectedMission = UserSelectedMission.create(
                user,
                selectedMission,
                currentSolarTerm,
                today
        );
        userSelectedMissionRepository.save(userSelectedMission);

        return SelectedMissionResponse.from(selectedMission);
    }
}
