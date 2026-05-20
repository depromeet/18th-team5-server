package com.team.peektime_api.domain.home.service;

import com.team.peektime_api.domain.home.dto.HomeResponse;
import com.team.peektime_api.domain.mission.repository.DailyMissionRepository;
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

    public HomeResponse getHome() {
        LocalDate today = LocalDate.now();

        return dailyMissionRepository.findByMissionDateWithDetails(today)
                .map(HomeResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_MISSION_NOT_FOUND));
    }
}