package com.team.peektime_admin.domain.mission.entity;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.global.common.BaseEntity;
import com.team.peektime_admin.global.common.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommended_mission_pool", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mission_term_user", columnNames = {"mission_id", "solar_term_id", "user_type"})
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

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Builder
    public RecommendedMissionPool(Mission mission, SolarTerm solarTerm, UserType userType, Integer displayOrder) {
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.userType = userType;
        this.displayOrder = displayOrder;
    }

    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }
}
