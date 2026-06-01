package com.team.peektime_api.domain.home.service;

import com.team.peektime_api.domain.home.dto.HomeResponse;
import com.team.peektime_api.domain.mission.entity.DailyMission;
import com.team.peektime_api.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeCardService {

    private final DailyMissionRepository dailyMissionRepository;
    private final UserMissionCompletionRepository userMissionCompletionRepository;

    public HomeResponse getHome(Long userId) {
        LocalDate today = LocalDate.now();

        DailyMission dailyMission = dailyMissionRepository.findByMissionDateWithDetails(today)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_MISSION_NOT_FOUND));

        boolean isCompleted = userMissionCompletionRepository
                .existsByUser_IdAndMission_Id(userId, dailyMission.getMission().getId());

        return HomeResponse.from(dailyMission, isCompleted);
    }
}