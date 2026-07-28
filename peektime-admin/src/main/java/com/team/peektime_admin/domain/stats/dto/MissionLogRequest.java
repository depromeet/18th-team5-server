package com.team.peektime_admin.domain.stats.dto;

import java.time.LocalDate;

public record MissionLogRequest(
        String idempotencyKey,
        Long userId,
        Long solarTermId,
        LocalDate completedDate
) {
}