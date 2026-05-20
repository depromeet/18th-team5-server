package com.team.peektime_api.domain.home.dto;

import com.team.peektime_api.domain.mission.entity.DailyMission;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.global.common.enums.MissionType;

import java.time.LocalDate;

public record HomeResponse(
        SolarTermInfo solarTerm,
        DailyMissionInfo dailyMission
) {

    public static HomeResponse from(DailyMission dailyMission) {
        return new HomeResponse(
                SolarTermInfo.from(dailyMission.getSolarTerm()),
                DailyMissionInfo.from(dailyMission)
        );
    }

    public record SolarTermInfo(
            Long id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {
        public static SolarTermInfo from(SolarTerm solarTerm) {
            return new SolarTermInfo(
                    solarTerm.getId(),
                    solarTerm.getName(),
                    solarTerm.getDescription(),
                    solarTerm.getStartDate(),
                    solarTerm.getEndDate()
            );
        }
    }

    public record DailyMissionInfo(
            Long id,
            String title,
            Integer participantCount,
            MissionType missionType
    ) {
        public static DailyMissionInfo from(DailyMission dailyMission) {
            return new DailyMissionInfo(
                    dailyMission.getMission().getId(),
                    dailyMission.getMission().getTitle(),
                    dailyMission.getParticipantCount(),
                    MissionType.DAILY
            );
        }
    }
}