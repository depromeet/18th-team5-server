package com.team.peektime_api.domain.sync.service;

import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_api.domain.mission.repository.MissionRepository;
import com.team.peektime_api.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.sync.dto.RecommendedMissionSyncDto;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
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

        recommendedMissionPoolRepository.deleteBySolarTermId(solarTermId);

        SolarTerm solarTerm = solarTermRepository.findById(solarTermId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));

        for (RecommendedMissionSyncDto dto : dtos) {
            Mission mission = missionRepository.findById(dto.missionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

            RecommendedMissionPool recommendedMission = RecommendedMissionPool.create(
                    dto.id(), mission, solarTerm, dto.userType(), dto.displayOrder()
            );
            recommendedMissionPoolRepository.save(recommendedMission);
        }

        log.info("추천 미션 동기화 완료: solarTermId={}, count={}", solarTermId, dtos.size());
    }
}