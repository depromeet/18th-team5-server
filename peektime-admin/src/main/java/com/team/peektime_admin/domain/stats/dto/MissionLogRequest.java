package com.team.peektime_admin.domain.stats.dto;

import com.team.peektime_admin.global.common.enums.MissionType;

import java.time.LocalDateTime;

public record MissionLogRequest(
        String idempotencyKey,
        String userUuid,
        Long missionId,
        MissionType missionType,
        Long solarTermId,
        LocalDateTime completedAt
) {
}