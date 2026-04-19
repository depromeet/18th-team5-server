package com.team.peektime_admin.domain.solarterm.entity;

import com.team.peektime_admin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "solar_term", uniqueConstraints = {
        @UniqueConstraint(name = "uk_solar_term", columnNames = {"name", "year"})
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

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "seasonal_foods", columnDefinition = "TEXT")
    private String seasonalFoods;

    @Column(name = "recommended_activities", columnDefinition = "TEXT")
    private String recommendedActivities;

    @Builder
    public SolarTerm(String name, String description, LocalDate startDate, LocalDate endDate,
                     Integer year, String seasonalFoods, String recommendedActivities) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.year = year;
        this.seasonalFoods = seasonalFoods;
        this.recommendedActivities = recommendedActivities;
    }
}
