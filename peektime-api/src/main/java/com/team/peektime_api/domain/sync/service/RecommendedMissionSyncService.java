package com.team.peektime_api.domain.sync.service;

import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_api.domain.mission.repository.MissionRepository;
import com.team.peektime_api.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.sync.dto.RecommendedMissionSyncDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendedMissionSyncService {

    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;
    private final MissionRepository missionRepository;
    private final SolarTermRepository solarTermRepository;

    @Transactional
    public void syncRecommendedMissions(Long solarTermId, List<RecommendedMissionSyncDto> dtos) {
        log.info("추천 미션 동기화 시작: solarTermId={}, count={}", solarTermId, dtos.size());

        // 기존 데이터 삭제 후 새로 삽입 (전체 교체)
        recommendedMissionPoolRepository.deleteBySolarTermId(solarTermId);

        SolarTerm solarTerm = solarTermRepository.findById(solarTermId)
                .orElseThrow(() -> new IllegalArgumentException("절기를 찾을 수 없습니다: " + solarTermId));

        for (RecommendedMissionSyncDto dto : dtos) {
            Mission mission = missionRepository.findById(dto.missionId())
                    .orElseThrow(() -> new IllegalArgumentException("미션을 찾을 수 없습니다: " + dto.missionId()));

            RecommendedMissionPool recommendedMission = RecommendedMissionPool.create(
                    dto.id(),
                    mission,
                    solarTerm,
                    dto.userType(),
                    dto.displayOrder()
            );

            recommendedMissionPoolRepository.save(recommendedMission);
        }

        log.info("추천 미션 동기화 완료: solarTermId={}, count={}", solarTermId, dtos.size());
    }
}