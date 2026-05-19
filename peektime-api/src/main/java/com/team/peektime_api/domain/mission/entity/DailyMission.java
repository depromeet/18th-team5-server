package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_mission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_mission_date", columnNames = {"solar_term_id", "mission_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyMission extends BaseEntity {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solar_term_id", nullable = false)
    private SolarTerm solarTerm;

    @Column(name = "mission_date")
    private LocalDate missionDate;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Builder(access = AccessLevel.PRIVATE)
    private DailyMission(Long id, Mission mission, SolarTerm solarTerm, LocalDate missionDate, Integer participantCount) {
        this.id = id;
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.missionDate = missionDate;
        this.participantCount = participantCount != null ? participantCount : 0;
    }

    public static DailyMission create(Long id, Mission mission, SolarTerm solarTerm, LocalDate missionDate) {
        return DailyMission.builder()
                .id(id)
                .mission(mission)
                .solarTerm(solarTerm)
                .missionDate(missionDate)
                .participantCount(0)
                .build();
    }

    public void incrementParticipantCount() {
        this.participantCount++;
    }

    public void update(Mission mission, SolarTerm solarTerm, LocalDate missionDate) {
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.missionDate = missionDate;
    }

    public boolean isAssigned() {
        return this.missionDate != null;
    }
}