package com.team.peektime_admin.domain.mission.entity;

import com.team.peektime_admin.global.common.BaseEntity;
import com.team.peektime_admin.global.common.enums.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false)
    private SpaceType spaceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "intensity_type", nullable = false)
    private IntensityType intensityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private CategoryType categoryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "companion_type", nullable = false)
    private CompanionType companionType = CompanionType.SOLO;

    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type", nullable = false)
    private EnjoyType enjoyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MissionStatus status = MissionStatus.POOL;

    @Builder
    public Mission(String title, String description, SpaceType spaceType,
                   IntensityType intensityType, CategoryType categoryType,
                   CompanionType companionType, EnjoyType enjoyType,
                   UserType userType, MissionStatus status) {
        this.title = title;
        this.description = description;
        this.spaceType = spaceType;
        this.intensityType = intensityType;
        this.categoryType = categoryType;
        this.companionType = companionType != null ? companionType : CompanionType.SOLO;
        this.enjoyType = enjoyType;
        this.userType = userType;
        this.status = status != null ? status : MissionStatus.POOL;
    }

    public void moveToPending() {
        this.status = MissionStatus.PENDING;
    }

    public void assign() {
        this.status = MissionStatus.ASSIGNED;
    }

    public void returnToPool() {
        this.status = MissionStatus.POOL;
    }
}
