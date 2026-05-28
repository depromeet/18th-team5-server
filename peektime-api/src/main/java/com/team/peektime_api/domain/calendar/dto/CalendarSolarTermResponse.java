package com.team.peektime_api.domain.calendar.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarSolarTermResponse(

        List<SolarTermEntry> solarTerms,
        Long prevSolarTermId,
        Long nextSolarTermId
) {

    public record SolarTermEntry(
            Long solarTermId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            List<DateEntry> dates
    ) {}

    public record DateEntry(
            LocalDate date,
            String thumbnailUrl
    ) {}
}
