package com.team.peektime_api.domain.stats.entity;

import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.MissionType;
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

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Column(name = "solar_term_id", nullable = false)
    private Long solarTermId;

    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Column(name = "completion_count", nullable = false)
    private Integer completionCount = 0;

    @Builder
    public MissionCompletionStats(Long missionId, MissionType missionType, Long solarTermId,
                                   LocalDate referenceDate, Integer completionCount) {
        this.missionId = missionId;
        this.missionType = missionType;
        this.solarTermId = solarTermId;
        this.referenceDate = referenceDate;
        this.completionCount = completionCount != null ? completionCount : 0;
    }

    public void incrementCount() {
        this.completionCount++;
    }
}
