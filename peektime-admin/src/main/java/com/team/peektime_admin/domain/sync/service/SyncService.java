package com.team.peektime_admin.domain.sync.service;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_admin.infra.api.ApiSyncClient;
import com.team.peektime_admin.infra.api.dto.DailyMissionSyncDto;
import com.team.peektime_admin.infra.api.dto.MissionSyncDto;
import com.team.peektime_admin.infra.api.dto.RecommendedMissionSyncDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final MissionRepository missionRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;
    private final ApiSyncClient apiSyncClient;

    @Transactional(readOnly = true)
    public void syncAllMissions() {
        List<Mission> missions = missionRepository.findAll();
        List<MissionSyncDto> dtos = missions.stream()
                .map(MissionSyncDto::from)
                .toList();

        apiSyncClient.syncMissions(dtos);
        log.info("전체 미션 동기화 완료: {}개", dtos.size());
    }

    @Transactional(readOnly = true)
    public void syncDailyMissions(Long solarTermId) {
        List<DailyMission> dailyMissions = dailyMissionRepository.findBySolarTermId(solarTermId);
        List<DailyMissionSyncDto> dtos = dailyMissions.stream()
                .map(DailyMissionSyncDto::from)
                .toList();

        apiSyncClient.syncDailyMissions(solarTermId, dtos);
        log.info("오늘의 미션 동기화 완료: solarTermId={}, count={}", solarTermId, dtos.size());
    }

    @Transactional(readOnly = true)
    public void syncRecommendedMissions(Long solarTermId) {
        List<RecommendedMissionPool> recommendedMissions = recommendedMissionPoolRepository.findBySolarTermId(solarTermId);
        List<RecommendedMissionSyncDto> dtos = recommendedMissions.stream()
                .map(RecommendedMissionSyncDto::from)
                .toList();

        apiSyncClient.syncRecommendedMissions(solarTermId, dtos);
        log.info("추천 미션 동기화 완료: solarTermId={}, count={}", solarTermId, dtos.size());
    }
}