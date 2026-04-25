package com.team.peektime_admin.domain.solarterm.dto;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class SolarTermResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer year;
    private final LocalDateTime createdAt;

    public SolarTermResponse(SolarTerm solarTerm) {
        this.id = solarTerm.getId();
        this.name = solarTerm.getName();
        this.description = solarTerm.getDescription();
        this.startDate = solarTerm.getStartDate();
        this.endDate = solarTerm.getEndDate();
        this.year = solarTerm.getYear();
        this.createdAt = solarTerm.getCreatedAt();
    }
}
