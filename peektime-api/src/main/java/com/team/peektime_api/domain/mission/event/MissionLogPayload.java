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
    public static MissionLogPayload from(MissionCompletedEvent event) {
        return new MissionLogPayload(
                event.userUuid(),
                event.missionId(),
                event.missionType(),
                event.solarTermId(),
                event.completedAt()
        );
    }
}