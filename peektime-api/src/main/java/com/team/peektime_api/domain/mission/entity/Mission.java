package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.*;
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
    private CompanionType companionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type")
    private EnjoyType enjoyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Builder(access = AccessLevel.PRIVATE)
    private Mission(Long id, String title, String description, SpaceType spaceType,
                    IntensityType intensityType, CategoryType categoryType,
                    CompanionType companionType, EnjoyType enjoyType, UserType userType,
                    boolean deleted) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.spaceType = spaceType;
        this.intensityType = intensityType;
        this.categoryType = categoryType;
        this.companionType = companionType;
        this.enjoyType = enjoyType;
        this.userType = userType;
        this.deleted = deleted;
    }

    public static Mission create(Long id, String title, String description, SpaceType spaceType,
                                  IntensityType intensityType, CategoryType categoryType,
                                  CompanionType companionType, EnjoyType enjoyType, UserType userType) {
        return Mission.builder()
                .id(id)
                .title(title)
                .description(description)
                .spaceType(spaceType)
                .intensityType(intensityType)
                .categoryType(categoryType)
                .companionType(companionType)
                .enjoyType(enjoyType)
                .userType(userType)
                .deleted(false)
                .build();
    }

    public void update(String title, String description, SpaceType spaceType,
                       IntensityType intensityType, CategoryType categoryType,
                       CompanionType companionType, EnjoyType enjoyType, UserType userType,
                       boolean deleted) {
        this.title = title;
        this.description = description;
        this.spaceType = spaceType;
        this.intensityType = intensityType;
        this.categoryType = categoryType;
        this.companionType = companionType;
        this.enjoyType = enjoyType;
        this.userType = userType;
        this.deleted = deleted;
    }
}