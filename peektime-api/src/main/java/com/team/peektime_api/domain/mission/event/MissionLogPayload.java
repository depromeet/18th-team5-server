package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.global.common.enums.MissionType;

import java.time.LocalDateTime;

public record MissionLogPayload(
        String userUuid,
        Long missionId,
        MissionType missionType,
        Long solarTermId,
        LocalDateTime completedAt
) {
    public static MissionLogPayload of(
            String userUuid,
            Long missionId,
            MissionType missionType,
            Long solarTermId,
            LocalDateTime completedAt
    ) {
        return new MissionLogPayload(userUuid, missionId, missionType, solarTermId, completedAt);
    }
}