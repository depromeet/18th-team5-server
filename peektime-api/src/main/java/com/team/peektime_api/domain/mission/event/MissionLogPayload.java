package com.team.peektime_api.domain.mission.event;

public record MissionLogPayload(
        String idempotencyKey,
        String userUuid,
        Long solarTermId
) {
    public static MissionLogPayload of(
            String idempotencyKey,
            String userUuid,
            Long solarTermId
    ) {
        return new MissionLogPayload(idempotencyKey, userUuid, solarTermId);
    }
}