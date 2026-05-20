package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommended_mission_pool", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recommended_mission", columnNames = {"mission_id", "solar_term_id", "user_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendedMissionPool extends BaseEntity {

    @Id
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

    @Builder(access = AccessLevel.PRIVATE)
    private RecommendedMissionPool(Long id, Mission mission, SolarTerm solarTerm,
                                    UserType userType, Integer displayOrder) {
        this.id = id;
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.userType = userType;
        this.displayOrder = displayOrder;
    }

    public static RecommendedMissionPool create(Long id, Mission mission, SolarTerm solarTerm,
                                                 UserType userType, Integer displayOrder) {
        return RecommendedMissionPool.builder()
                .id(id)
                .mission(mission)
                .solarTerm(solarTerm)
                .userType(userType)
                .displayOrder(displayOrder)
                .build();
    }

    public void update(Mission mission, SolarTerm solarTerm, UserType userType, Integer displayOrder) {
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.userType = userType;
        this.displayOrder = displayOrder;
    }
}