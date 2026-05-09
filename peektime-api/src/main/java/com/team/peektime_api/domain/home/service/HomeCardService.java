package com.team.peektime_api.domain.home.service;

import com.team.peektime_api.domain.home.dto.HomeResponse;
import com.team.peektime_api.domain.home.dto.HomeResponse.DailyMissionInfo;
import com.team.peektime_api.domain.home.dto.HomeResponse.SolarTermInfo;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import com.team.peektime_api.global.infra.cache.DailyMissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeCardService {

    private final AdminClient adminClient;
    private final DailyMissionCacheService cacheService;
    private final UserMissionCompletionRepository userMissionCompletionRepository;

    public HomeResponse getHome() {
        AdminHomeResponse adminData = getAdminDataWithCache();

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

    private AdminHomeResponse getAdminDataWithCache() {
        LocalDate today = LocalDate.now();

        return cacheService.get(today)
                .orElseGet(() -> {
                    log.info("캐시 미스 - Admin API 호출: {}", today);
                    AdminHomeResponse data = adminClient.getHomeData(today);
                    cacheService.save(today, data);
                    return data;
                });
    }
}