package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_mission_stats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_mission_stats", columnNames = {"mission_id", "mission_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyMissionStats extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "solar_term_id", nullable = false)
    private Long solarTermId;

    @Column(name = "mission_date", nullable = false)
    private LocalDate missionDate;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Builder(access = AccessLevel.PRIVATE)
    private DailyMissionStats(Long missionId, Long solarTermId, LocalDate missionDate, Integer participantCount) {
        this.missionId = missionId;
        this.solarTermId = solarTermId;
        this.missionDate = missionDate;
        this.participantCount = participantCount != null ? participantCount : 0;
    }

    public static DailyMissionStats create(Long missionId, Long solarTermId, LocalDate missionDate) {
        return DailyMissionStats.builder()
                .missionId(missionId)
                .solarTermId(solarTermId)
                .missionDate(missionDate)
                .participantCount(0)
                .build();
    }

    public void incrementCount() {
        this.participantCount++;
    }

    public void updateCount(Integer count) {
        this.participantCount = count;
    }
}