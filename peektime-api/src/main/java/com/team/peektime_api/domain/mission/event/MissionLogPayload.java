package com.team.peektime_api.domain.mission.event;

public record MissionLogPayload(
        String idempotencyKey,
        Long userId,
        Long solarTermId
) {
    public static MissionLogPayload of(
            String idempotencyKey,
            Long userId,
            Long solarTermId
    ) {
        return new MissionLogPayload(idempotencyKey, userId, solarTermId);
    }
}