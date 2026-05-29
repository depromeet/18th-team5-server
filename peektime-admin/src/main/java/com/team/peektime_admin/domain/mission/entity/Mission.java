package com.team.peektime_admin.domain.mission.entity;

import com.team.peektime_admin.domain.mission.dto.MissionRequest;
import com.team.peektime_admin.global.common.BaseEntity;
import com.team.peektime_admin.global.common.enums.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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



    // 20자 이내로 받아야한다.
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 필터링용 태그
    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false)
    private SpaceType spaceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private CategoryType categoryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "companion_type", nullable = false)
    private CompanionType companionType;

    // 추천미션 분류용 태그
    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type")
    private EnjoyType enjoyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    // Soft Delete
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Mission(String title, String description, SpaceType spaceType,
                   CategoryType categoryType, CompanionType companionType,
                   EnjoyType enjoyType, UserType userType) {
        this.title = title;
        this.description = description;
        this.spaceType = spaceType;
        this.categoryType = categoryType;
        this.companionType = companionType != null ? companionType : CompanionType.SOLO;
        this.enjoyType = enjoyType;
        this.userType = userType;
    }

    public static Mission create(MissionRequest request) {
        return Mission.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .spaceType(request.getSpaceType())
                .companionType(request.getCompanionType())
                .categoryType(request.getCategoryType())
                .enjoyType(request.getEnjoyType())
                .userType(request.getUserType())
                .build();
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }

    public void update(String title, String description, SpaceType spaceType,
                       CategoryType categoryType, CompanionType companionType,
                       EnjoyType enjoyType, UserType userType) {
        this.title = title;
        this.description = description;
        this.spaceType = spaceType;
        this.categoryType = categoryType;
        this.companionType = companionType != null ? companionType : CompanionType.SOLO;
        this.enjoyType = enjoyType;
        this.userType = userType;
    }
}
