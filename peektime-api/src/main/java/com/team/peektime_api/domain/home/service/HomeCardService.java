package com.team.peektime_api.domain.home.service;

import com.team.peektime_api.domain.home.dto.HomeResponse;
import com.team.peektime_api.domain.home.dto.HomeResponse.DailyMissionInfo;
import com.team.peektime_api.domain.home.dto.HomeResponse.SolarTermInfo;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeCardService {

    private final AdminClient adminClient;
    private final UserMissionCompletionRepository userMissionCompletionRepository;

    public HomeResponse getHome() {
        AdminHomeResponse adminData = adminClient.getHomeData();

        SolarTermInfo solarTermInfo = null;
        if (adminData.solarTerm() != null) {
            solarTermInfo = SolarTermInfo.from(adminData.solarTerm());
        }

        DailyMissionInfo dailyMissionInfo = null;
        if (adminData.dailyMission() != null) {
            long participantCount = userMissionCompletionRepository
                    .countByMissionId(adminData.dailyMission().id());
            dailyMissionInfo = DailyMissionInfo.from(adminData.dailyMission(), participantCount);
        }

        return new HomeResponse(solarTermInfo, dailyMissionInfo);
    }
}