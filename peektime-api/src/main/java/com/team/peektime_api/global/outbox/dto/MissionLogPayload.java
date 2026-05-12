package com.team.peektime_api.global.outbox.dto;

import com.team.peektime_api.global.common.enums.MissionType;

import java.time.LocalDateTime;

public record MissionLogPayload(
        String userUuid,
        Long missionId,
        MissionType missionType,
        Long solarTermId,
        LocalDateTime completedAt
) {
}