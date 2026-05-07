package com.team.peektime_admin.domain.home.service;

import com.team.peektime_admin.domain.home.dto.HomeDataResponse;
import com.team.peektime_admin.domain.mission.entity.DailyMission;
import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeDataService {

    private final SolarTermRepository solarTermRepository;
    private final DailyMissionRepository dailyMissionRepository;

    public HomeDataResponse getHomeData() {
        LocalDate today = LocalDate.now();

        SolarTerm currentSolarTerm = solarTermRepository.findByDate(today)
                .orElse(null);

        DailyMission todayMission = null;
        if (currentSolarTerm != null) {
            todayMission = dailyMissionRepository
                    .findBySolarTermIdAndMissionDate(currentSolarTerm.getId(), today)
                    .orElse(null);
        }

        return HomeDataResponse.of(currentSolarTerm, todayMission);
    }
}