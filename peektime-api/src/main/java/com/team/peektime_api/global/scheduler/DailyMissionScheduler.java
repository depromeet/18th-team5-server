package com.team.peektime_api.global.scheduler;

import com.team.peektime_api.domain.mission.entity.DailyMissionStats;
import com.team.peektime_api.domain.mission.repository.DailyMissionStatsRepository;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import com.team.peektime_api.global.infra.cache.DailyMissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMissionScheduler {

    private final AdminClient adminClient;
    private final DailyMissionCacheService cacheService;
    private final DailyMissionStatsRepository dailyMissionStatsRepository;

    /**
     * 매일 22시에 다음날 미션을 캐시에 저장
     */
    @Scheduled(cron = "0 0 22 * * *")
    public void preloadTomorrowMission() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        loadAndCacheMission(tomorrow, "22:00 정기 스케줄");
    }

    /**
     * 23시 재시도 - 캐시에 없을 경우만 실행
     */
    @Scheduled(cron = "0 0 23 * * *")
    public void retryAt23() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (!cacheService.exists(tomorrow)) {
            loadAndCacheMission(tomorrow, "23:00 재시도");
        }
    }

    /**
     * 00시 마지막 재시도 - 캐시에 없을 경우만 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void retryAtMidnight() {
        LocalDate today = LocalDate.now();
        if (!cacheService.exists(today)) {
            loadAndCacheMission(today, "00:00 마지막 재시도");
        }
    }

    private void loadAndCacheMission(LocalDate date, String context) {
        try {
            log.info("[{}] {} 미션 캐시 로드 시작", context, date);
            AdminHomeResponse data = adminClient.getHomeData(date);
            cacheService.save(date, data);
            log.info("[{}] {} 미션 캐시 저장 완료", context, date);

            createDailyMissionStatsIfNotExists(data, date);
        } catch (Exception e) {
            log.error("[{}] {} 미션 캐시 로드 실패: {}", context, date, e.getMessage());
        }
    }

    private void createDailyMissionStatsIfNotExists(AdminHomeResponse data, LocalDate date) {
        if (data.dailyMission() == null || data.solarTerm() == null) {
            return;
        }

        Long missionId = data.dailyMission().id();
        Long solarTermId = data.solarTerm().id();

        if (!dailyMissionStatsRepository.existsByMissionIdAndMissionDate(missionId, date)) {
            DailyMissionStats stats = DailyMissionStats.create(missionId, solarTermId, date);
            dailyMissionStatsRepository.save(stats);
            log.info("DailyMissionStats 생성 완료: missionId={}, date={}", missionId, date);
        }
    }
}