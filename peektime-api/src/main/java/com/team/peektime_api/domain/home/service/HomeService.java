package com.team.peektime_api.domain.home.service;

import com.team.peektime_api.domain.home.dto.HomeResponse;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.common.enums.MissionType;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final AdminClient adminClient;
    private final UserMissionCompletionRepository userMissionCompletionRepository;

    public HomeResponse getHome() {
        AdminHomeResponse adminData = adminClient.getHomeData();

        HomeResponse.SolarTermInfo solarTermInfo = null;
        if (adminData.solarTerm() != null) {
            solarTermInfo = new HomeResponse.SolarTermInfo(
                    adminData.solarTerm().id(),
                    adminData.solarTerm().name(),
                    adminData.solarTerm().description(),
                    adminData.solarTerm().startDate(),
                    adminData.solarTerm().endDate()
            );
        }

        HomeResponse.DailyMissionInfo dailyMissionInfo = null;
        if (adminData.dailyMission() != null) {
            long participantCount = userMissionCompletionRepository
                    .countByMissionId(adminData.dailyMission().id());

            dailyMissionInfo = new HomeResponse.DailyMissionInfo(
                    adminData.dailyMission().id(),
                    adminData.dailyMission().title(),
                    participantCount,
                    MissionType.DAILY
            );
        }

        return new HomeResponse(solarTermInfo, dailyMissionInfo);
    }
}