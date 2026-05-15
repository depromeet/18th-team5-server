package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.global.common.enums.MissionType;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;

import java.time.LocalDateTime;

public record MissionCompletedEvent(
        Long outboxId,
        String userUuid,
        Long missionId,
        MissionType missionType,
        Long solarTermId,
        LocalDateTime completedAt
) {
    public static MissionCompletedEvent from(OutboxEvent outbox, MissionLogPayload payload) {
        return new MissionCompletedEvent(
                outbox.getId(),
                payload.userUuid(),
                payload.missionId(),
                payload.missionType(),
                payload.solarTermId(),
                payload.completedAt()
        );
    }
}