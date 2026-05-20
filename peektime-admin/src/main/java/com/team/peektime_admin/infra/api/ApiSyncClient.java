package com.team.peektime_admin.infra.api;

import com.team.peektime_admin.infra.api.dto.DailyMissionSyncDto;
import com.team.peektime_admin.infra.api.dto.MissionSyncDto;
import com.team.peektime_admin.infra.api.dto.RecommendedMissionSyncDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSyncClient {

    private final RestClient apiRestClient;

    public void syncMissions(List<MissionSyncDto> missions) {
        try {
            apiRestClient.post()
                    .uri("/internal/sync/missions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(missions)
                    .retrieve()
                    .toBodilessEntity();
            log.info("미션 동기화 완료: {}개", missions.size());
        } catch (Exception e) {
            log.error("미션 동기화 실패: {}", e.getMessage());
            throw new RuntimeException("미션 동기화 실패", e);
        }
    }

    public void syncDailyMissions(Long solarTermId, List<DailyMissionSyncDto> dailyMissions) {
        try {
            apiRestClient.post()
                    .uri("/internal/sync/daily-missions?solarTermId={id}", solarTermId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dailyMissions)
                    .retrieve()
                    .toBodilessEntity();
            log.info("오늘의 미션 동기화 완료: solarTermId={}, count={}", solarTermId, dailyMissions.size());
        } catch (Exception e) {
            log.error("오늘의 미션 동기화 실패: {}", e.getMessage());
            throw new RuntimeException("오늘의 미션 동기화 실패", e);
        }
    }

    public void syncRecommendedMissions(Long solarTermId, List<RecommendedMissionSyncDto> recommendedMissions) {
        try {
            apiRestClient.post()
                    .uri("/internal/sync/recommended-missions?solarTermId={id}", solarTermId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(recommendedMissions)
                    .retrieve()
                    .toBodilessEntity();
            log.info("추천 미션 동기화 완료: solarTermId={}, count={}", solarTermId, recommendedMissions.size());
        } catch (Exception e) {
            log.error("추천 미션 동기화 실패: {}", e.getMessage());
            throw new RuntimeException("추천 미션 동기화 실패", e);
        }
    }
}