package com.team.peektime_api.global.outbox.event;

import com.team.peektime_api.global.common.enums.MissionType;

import java.time.LocalDateTime;

public record MissionCompletedEvent(
        Long outboxId,
        String userUuid,
        Long missionId,
        MissionType missionType,
        Long solarTermId,
        LocalDateTime completedAt
) {
}