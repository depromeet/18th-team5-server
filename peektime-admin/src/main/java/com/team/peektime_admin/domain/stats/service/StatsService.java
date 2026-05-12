package com.team.peektime_admin.domain.stats.service;

import com.team.peektime_admin.domain.stats.dto.MissionLogRequest;
import com.team.peektime_admin.domain.stats.entity.UserMissionLog;
import com.team.peektime_admin.domain.stats.repository.UserMissionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMissionLogRepository userMissionLogRepository;

    @Transactional
    public void saveMissionLog(MissionLogRequest request) {
        UserMissionLog missionLog = UserMissionLog.builder()
                .userUuid(request.userUuid())
                .missionId(request.missionId())
                .missionType(request.missionType())
                .solarTermId(request.solarTermId())
                .completedDate(request.completedAt().toLocalDate())
                .completedAt(request.completedAt())
                .build();

        userMissionLogRepository.save(missionLog);
        log.info("미션 로그 저장 완료: userUuid={}, missionId={}", request.userUuid(), request.missionId());
    }
}