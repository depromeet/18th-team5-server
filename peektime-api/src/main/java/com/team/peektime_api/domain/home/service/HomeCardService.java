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
import java.util.Optional;

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

        SolarTermInfo solarTermInfo = Optional.ofNullable(adminData.solarTerm())
                .map(SolarTermInfo::from)
                .orElse(null);

        DailyMissionInfo dailyMissionInfo = Optional.ofNullable(adminData.dailyMission())
                .map(this::createDailyMissionInfo)
                .orElse(null);

        return new HomeResponse(solarTermInfo, dailyMissionInfo);
    }

    private DailyMissionInfo createDailyMissionInfo(AdminHomeResponse.DailyMissionData data) {
        long participantCount = userMissionCompletionRepository.countByMissionId(data.id());
        return DailyMissionInfo.from(data, participantCount);
    }

    private AdminHomeResponse getAdminDataWithCache() {
        LocalDate today = LocalDate.now();

        return cacheService.get(today)
                .orElseGet(() -> { // fallBack : 안정 장치 용으로 Admin 쪽으로 API call();
                    log.info("캐시 미스 - Admin API 호출: {}", today);
                    AdminHomeResponse data = adminClient.getHomeData(today);
                    cacheService.save(today, data);
                    return data;
                });
    }
}