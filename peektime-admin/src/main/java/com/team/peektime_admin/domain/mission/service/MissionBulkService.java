package com.team.peektime_admin.domain.mission.service;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final int MAX_MISSIONS_PER_ENJOY_TYPE = 5;

    public void moveToRecommendedMission(List<Long> missionIds, Long solarTermId, String userTypeStr) {
        SolarTerm solarTerm = solarTermRepository.findById(solarTermId)
                .orElseThrow(() -> new IllegalArgumentException("절기를 찾을 수 없습니다."));

        UserType userType = UserType.valueOf(userTypeStr);

        List<Mission> missions = missionRepository.findAllById(missionIds);

        // 1. 오늘의 미션에 이미 배정된 미션이 있는지 확인
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

        // 2. EnjoyType별 5개 제한 확인
        Map<EnjoyType, Long> currentCounts = new HashMap<>();
        Map<EnjoyType, Long> addCounts = new HashMap<>();

        for (Mission mission : missions) {
            if (mission.getEnjoyType() == null) {
                throw new IllegalStateException(
                        "미션 '" + mission.getTitle() + "'에 즐기기 타입(EnjoyType)이 설정되지 않았습니다.");
            }

            EnjoyType enjoyType = mission.getEnjoyType();

            // 현재 카운트 조회 (캐싱)
            if (!currentCounts.containsKey(enjoyType)) {
                long count = recommendedMissionPoolRepository.countBySolarTermIdAndUserTypeAndEnjoyType(
                        solarTermId, userType, enjoyType);
                currentCounts.put(enjoyType, count);
            }

            // 추가하려는 개수 카운트
            addCounts.merge(enjoyType, 1L, Long::sum);
        }

        // 제한 초과 확인
        List<String> exceededTypes = new ArrayList<>();
        for (Map.Entry<EnjoyType, Long> entry : addCounts.entrySet()) {
            EnjoyType enjoyType = entry.getKey();
            long currentCount = currentCounts.getOrDefault(enjoyType, 0L);
            long addCount = entry.getValue();

            if (currentCount + addCount > MAX_MISSIONS_PER_ENJOY_TYPE) {
                long remaining = MAX_MISSIONS_PER_ENJOY_TYPE - currentCount;
                exceededTypes.add(String.format("%s (현재 %d개, 추가 %d개 요청, 남은 슬롯 %d개)",
                        enjoyType.getShortLabel(), currentCount, addCount, Math.max(0, remaining)));
            }
        }

        if (!exceededTypes.isEmpty()) {
            throw new IllegalStateException(
                    "즐기기 타입별 최대 " + MAX_MISSIONS_PER_ENJOY_TYPE + "개까지 배정 가능합니다.\n" +
                    "초과된 타입: " + String.join(", ", exceededTypes));
        }

        // 3. 모든 검증 통과 후 배정 진행
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