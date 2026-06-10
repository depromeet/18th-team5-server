package com.team.peektime_admin.domain.stats.dto;

public record MissionLogRequest(
        String idempotencyKey,
        Long userId,
        Long solarTermId
) {
}