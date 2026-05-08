package com.team.peektime_api.domain.home.dto;

import com.team.peektime_api.global.common.enums.MissionType;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse.DailyMissionData;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse.SolarTermData;

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
        public static SolarTermInfo from(SolarTermData data) {
            return new SolarTermInfo(
                    data.id(),
                    data.name(),
                    data.description(),
                    data.startDate(),
                    data.endDate()
            );
        }
    }

    public record DailyMissionInfo(
            Long id,
            String title,
            Long participantCount,
            MissionType missionType
    ) {
        public static DailyMissionInfo from(DailyMissionData data, Long participantCount) {
            return new DailyMissionInfo(
                    data.id(),
                    data.title(),
                    participantCount,
                    MissionType.DAILY
            );
        }
    }
}