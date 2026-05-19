package com.team.peektime_admin.domain.mission.entity;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_mission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_mission_term", columnNames = {"mission_id", "solar_term_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyMission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solar_term_id", nullable = false)
    private SolarTerm solarTerm;

    // NULL = 배정대기, 값 = 배정완료
    @Column(name = "mission_date")
    private LocalDate missionDate;

    @Builder
    public DailyMission(Mission mission, SolarTerm solarTerm, LocalDate missionDate) {
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.missionDate = missionDate;
    }

    public boolean isPending() {
        return this.missionDate == null;
    }

    public boolean isAssigned() {
        return this.missionDate != null;
    }

    public void assignToDate(LocalDate date) {
        this.missionDate = date;
    }

    public void unassign() {
        this.missionDate = null;
    }
}
