package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.global.common.enums.MissionType;

import java.time.LocalDateTime;

public record MissionLogPayload(
        String idempotencyKey,
        String userUuid,
        Long missionId,
        MissionType missionType,
        Long solarTermId,
        LocalDateTime completedAt
) {
    public static MissionLogPayload of(
            String idempotencyKey,
            String userUuid,
            Long missionId,
            MissionType missionType,
            Long solarTermId,
            LocalDateTime completedAt
    ) {
        return new MissionLogPayload(idempotencyKey, userUuid, missionId, missionType, solarTermId, completedAt);
    }
}