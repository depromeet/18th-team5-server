package com.team.peektime_admin.domain.stats.entity;

import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.global.common.BaseEntity;
import com.team.peektime_admin.global.common.enums.MissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "mission_completion_stats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stats", columnNames = {"mission_id", "mission_type", "solar_term_id", "reference_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionCompletionStats extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solar_term_id", nullable = false)
    private SolarTerm solarTerm;

    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Column(name = "completion_count", nullable = false)
    private Integer completionCount = 0;

    @Builder
    public MissionCompletionStats(Mission mission, MissionType missionType, SolarTerm solarTerm,
                                   LocalDate referenceDate, Integer completionCount) {
        this.mission = mission;
        this.missionType = missionType;
        this.solarTerm = solarTerm;
        this.referenceDate = referenceDate;
        this.completionCount = completionCount != null ? completionCount : 0;
    }

    public void incrementCount() {
        this.completionCount++;
    }
}
