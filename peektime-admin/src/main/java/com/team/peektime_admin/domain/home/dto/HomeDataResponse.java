package com.team.peektime_admin.domain.home.dto;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;

import java.time.LocalDate;

public record HomeDataResponse(
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
        public static SolarTermData from(SolarTerm solarTerm) {
            return new SolarTermData(
                    solarTerm.getId(),
                    solarTerm.getName(),
                    solarTerm.getDescription(),
                    solarTerm.getStartDate(),
                    solarTerm.getEndDate()
            );
        }
    }

    public record DailyMissionData(
            Long id,
            String title
    ) {
        public static DailyMissionData from(DailyMission dailyMission) {
            return new DailyMissionData(
                    dailyMission.getMission().getId(),
                    dailyMission.getMission().getTitle()
            );
        }
    }

    public static HomeDataResponse of(SolarTerm solarTerm, DailyMission dailyMission) {
        return new HomeDataResponse(
                solarTerm != null ? SolarTermData.from(solarTerm) : null,
                dailyMission != null ? DailyMissionData.from(dailyMission) : null
        );
    }
}