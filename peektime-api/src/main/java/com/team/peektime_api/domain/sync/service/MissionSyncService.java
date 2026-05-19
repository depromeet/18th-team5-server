package com.team.peektime_api.domain.sync.service;

import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.repository.MissionRepository;
import com.team.peektime_api.domain.sync.dto.MissionSyncDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionSyncService {

    private final MissionRepository missionRepository;

    @Transactional
    public void syncMissions(List<MissionSyncDto> dtos) {
        log.info("미션 동기화 시작: {}개", dtos.size());

        for (MissionSyncDto dto : dtos) {
            Mission mission = missionRepository.findById(dto.id())
                    .orElse(null);

            if (mission == null) {
                mission = Mission.create(
                        dto.id(),
                        dto.title(),
                        dto.description(),
                        dto.spaceType(),
                        dto.intensityType(),
                        dto.categoryType(),
                        dto.companionType(),
                        dto.enjoyType(),
                        dto.userType()
                );
            } else {
                mission.update(
                        dto.title(),
                        dto.description(),
                        dto.spaceType(),
                        dto.intensityType(),
                        dto.categoryType(),
                        dto.companionType(),
                        dto.enjoyType(),
                        dto.userType(),
                        dto.deleted()
                );
            }

            missionRepository.save(mission);
        }

        log.info("미션 동기화 완료: {}개", dtos.size());
    }
}