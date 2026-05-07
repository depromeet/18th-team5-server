package com.team.peektime_api.domain.home.dto;

import java.time.LocalDate;

public record HomeResponse(
        SolarTermInfo solarTerm,
        DailyMissionInfo dailyMission
) {

    public record SolarTermInfo(
            Long id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record DailyMissionInfo(
            Long id,
            String title,
            Long participantCount
    ) {
    }
}