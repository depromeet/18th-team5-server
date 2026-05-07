package com.team.peektime_api.global.infra.admin.dto;

import java.time.LocalDate;

public record AdminHomeResponse(
        SolarTermData solarTerm,
        DailyMissionData dailyMission
) {

    public record SolarTermData(
            Long id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record DailyMissionData(
            Long id,
            String title
    ) {
    }
}