package com.team.peektime_admin.domain.mission.entity;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommended_mission_pool", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mission_term", columnNames = {"mission_id", "solar_term_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendedMissionPool extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solar_term_id", nullable = false)
    private SolarTerm solarTerm;

    @Builder
    public RecommendedMissionPool(Mission mission, SolarTerm solarTerm) {
        this.mission = mission;
        this.solarTerm = solarTerm;
    }
}
