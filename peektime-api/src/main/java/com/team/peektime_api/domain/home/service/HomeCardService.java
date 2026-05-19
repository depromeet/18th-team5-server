package com.team.peektime_api.domain.home.service;

import com.team.peektime_api.domain.home.dto.HomeResponse;
import com.team.peektime_api.domain.home.dto.HomeResponse.DailyMissionInfo;
import com.team.peektime_api.domain.home.dto.HomeResponse.SolarTermInfo;
import com.team.peektime_api.domain.mission.entity.DailyMissionStats;
import com.team.peektime_api.domain.mission.repository.DailyMissionStatsRepository;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse.DailyMissionData;
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
    private final DailyMissionStatsRepository dailyMissionStatsRepository;

    @Transactional
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

    private DailyMissionInfo createDailyMissionInfo(DailyMissionData data) {
        long participantCount = userMissionCompletionRepository.countByMissionId(data.id());
        return DailyMissionInfo.from(data, participantCount);
    }

    private AdminHomeResponse getAdminDataWithCache() {
        LocalDate today = LocalDate.now();

        return cacheService.get(today)
                .orElseGet(() -> {
                    log.info("캐시 미스 - Admin API 호출: {}", today);
                    AdminHomeResponse data = adminClient.getHomeData(today);
                    if (data.dailyMission() != null) {
                        cacheService.save(today, data);
                        createDailyMissionStatsIfNotExists(data, today);
                    }
                    return data;
                });
    }

    private void createDailyMissionStatsIfNotExists(AdminHomeResponse data, LocalDate date) {
        if (data.dailyMission() == null || data.solarTerm() == null) {
            return;
        }

        Long missionId = data.dailyMission().id();
        Long solarTermId = data.solarTerm().id();

        try {
            if (!dailyMissionStatsRepository.existsByMissionIdAndMissionDate(missionId, date)) {
                DailyMissionStats stats = DailyMissionStats.create(missionId, solarTermId, date);
                dailyMissionStatsRepository.save(stats);
                log.info("DailyMissionStats 생성 완료: missionId={}, date={}", missionId, date);
            }
        } catch (Exception e) {
            log.warn("DailyMissionStats 생성 실패 (조회는 계속 진행): {}", e.getMessage());
        }
    }
}