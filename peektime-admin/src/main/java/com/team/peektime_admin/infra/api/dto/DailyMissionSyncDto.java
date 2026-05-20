package com.team.peektime_admin.infra.api.dto;

import com.team.peektime_admin.domain.mission.entity.DailyMission;

import java.time.LocalDate;

public record DailyMissionSyncDto(
        Long id,
        Long missionId,
        Long solarTermId,
        LocalDate missionDate
) {
    public static DailyMissionSyncDto from(DailyMission dailyMission) {
        return new DailyMissionSyncDto(
                dailyMission.getId(),
                dailyMission.getMission().getId(),
                dailyMission.getSolarTerm().getId(),
                dailyMission.getMissionDate()
        );
    }
}