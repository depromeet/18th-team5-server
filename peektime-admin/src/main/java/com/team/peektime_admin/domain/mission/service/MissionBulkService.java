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

        for (Mission mission : missions) {
            // 추천미션으로 이미 배정된 경우 오늘의 미션 배정 불가
            boolean isInRecommendedMission = recommendedMissionPoolRepository.existsByMissionId(mission.getId());
            if (isInRecommendedMission) {
                continue;
            }

            // 이미 오늘의 미션으로 등록되어 있는지 확인
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

        for (Mission mission : missions) {
            // 오늘의 미션으로 이미 배정된 경우 추천미션 배정 불가
            boolean isInDailyMission = dailyMissionRepository.existsByMissionId(mission.getId());
            if (isInDailyMission) {
                continue;
            }

            // 이미 추천 미션으로 등록되어 있는지 확인
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