package com.team.peektime_api.domain.sync.dto;

import java.time.LocalDate;

public record DailyMissionSyncDto(
        Long id,
        Long missionId,
        Long solarTermId,
        LocalDate missionDate
) {
}