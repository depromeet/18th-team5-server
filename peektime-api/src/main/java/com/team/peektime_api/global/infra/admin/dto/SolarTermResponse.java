package com.team.peektime_api.global.infra.admin.dto;

import java.time.LocalDate;

public record SolarTermResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {
}