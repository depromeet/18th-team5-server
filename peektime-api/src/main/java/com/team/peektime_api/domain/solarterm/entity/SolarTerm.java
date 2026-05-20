package com.team.peektime_api.domain.solarterm.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "solar_term", uniqueConstraints = {
        @UniqueConstraint(name = "uk_solar_term", columnNames = {"name", "solar_year"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolarTerm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "solar_year", nullable = false)
    private Integer year;

    @Builder(access = AccessLevel.PRIVATE)
    private SolarTerm(String name, String description, LocalDate startDate, LocalDate endDate, Integer year) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.year = year;
    }

    public static SolarTerm create(String name, String description, LocalDate startDate, LocalDate endDate, Integer year) {
        return SolarTerm.builder()
                .name(name)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .year(year)
                .build();
    }
}