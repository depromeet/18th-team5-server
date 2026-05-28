package com.team.peektime_admin.domain.mission.service;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionBulkService {

    private final MissionRepository missionRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;
    private final SolarTermRepository solarTermRepository;

    public void moveToDailyMission(List<Long> missionIds, Long solarTermId) {
        SolarTerm solarTerm = solarTermRepository.findById(solarTermId)
                .orElseThrow(() -> new IllegalArgumentException("절기를 찾을 수 없습니다."));

        List<Mission> missions = missionRepository.findAllById(missionIds);

        // 먼저 추천미션에 이미 배정된 미션이 있는지 확인
        List<String> conflictingMissions = new ArrayList<>();
        for (Mission mission : missions) {
            if (recommendedMissionPoolRepository.existsByMissionId(mission.getId())) {
                conflictingMissions.add(mission.getTitle());
            }
        }

        if (!conflictingMissions.isEmpty()) {
            throw new IllegalStateException(
                    "다음 미션이 이미 추천미션으로 배정되어 있습니다: " + String.join(", ", conflictingMissions));
        }

        // 모든 검증 통과 후 배정 진행
        for (Mission mission : missions) {
            boolean alreadyExists = dailyMissionRepository.existsByMissionId(mission.getId());
            if (!alreadyExists) {
                DailyMission dailyMission = DailyMission.builder()
                        .mission(mission)
                        .solarTerm(solarTerm)
                        .missionDate(null) // 배정대기 상태
                        .build();
                dailyMissionRepository.save(dailyMission);
            }
        }
    }

    public void moveToRecommendedMission(List<Long> missionIds, Long solarTermId, String userTypeStr) {
        SolarTerm solarTerm = solarTermRepository.findById(solarTermId)
                .orElseThrow(() -> new IllegalArgumentException("절기를 찾을 수 없습니다."));

        UserType userType = UserType.valueOf(userTypeStr);

        List<Mission> missions = missionRepository.findAllById(missionIds);

        // 먼저 오늘의 미션에 이미 배정된 미션이 있는지 확인
        List<String> conflictingMissions = new ArrayList<>();
        for (Mission mission : missions) {
            if (dailyMissionRepository.existsByMissionId(mission.getId())) {
                conflictingMissions.add(mission.getTitle());
            }
        }

        if (!conflictingMissions.isEmpty()) {
            throw new IllegalStateException(
                    "다음 미션이 이미 오늘의 미션으로 배정되어 있습니다: " + String.join(", ", conflictingMissions));
        }

        // 모든 검증 통과 후 배정 진행
        for (Mission mission : missions) {
            boolean alreadyExists = recommendedMissionPoolRepository.existsByMissionId(mission.getId());
            if (!alreadyExists) {
                RecommendedMissionPool recommendedMission = RecommendedMissionPool.builder()
                        .mission(mission)
                        .solarTerm(solarTerm)
                        .userType(userType)
                        .build();
                recommendedMissionPoolRepository.save(recommendedMission);
            }
        }
    }

    public void bulkDelete(List<Long> missionIds) {
        List<Mission> missions = missionRepository.findAllById(missionIds);
        for (Mission mission : missions) {
            mission.softDelete();
        }
    }

    public void deleteMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("미션을 찾을 수 없습니다."));
        mission.softDelete();
    }
}