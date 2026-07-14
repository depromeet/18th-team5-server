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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Admin DB에서 동기화된 미션의 원본 ID. 동기화 upsert 매칭 키로 사용하며, LLM 생성 미션은 null
    @Column(name = "admin_mission_id", unique = true)
    private Long adminMissionId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false)
    private SpaceType spaceType;

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

    // LLM이 특정 사용자를 위해 즉석 생성한 미션 여부. 공용 선택 미션 풀에서 제외된다.
    @Column(name = "llm_generated", nullable = false)
    private boolean llmGenerated = false;

    @Builder(access = AccessLevel.PRIVATE)
    private Mission(Long adminMissionId, String title, String description, SpaceType spaceType,
                    CategoryType categoryType, CompanionType companionType,
                    EnjoyType enjoyType, UserType userType, boolean deleted, boolean llmGenerated) {
        this.adminMissionId = adminMissionId;
        this.title = title;
        this.description = description;
        this.spaceType = spaceType;
        this.categoryType = categoryType;
        this.companionType = companionType;
        this.enjoyType = enjoyType;
        this.userType = userType;
        this.deleted = deleted;
        this.llmGenerated = llmGenerated;
    }

    public static Mission create(Long adminMissionId, String title, String description, SpaceType spaceType,
                                  CategoryType categoryType, CompanionType companionType,
                                  EnjoyType enjoyType, UserType userType) {
        return Mission.builder()
                .adminMissionId(adminMissionId)
                .title(title)
                .description(description)
                .spaceType(spaceType)
                .categoryType(categoryType)
                .companionType(companionType)
                .enjoyType(enjoyType)
                .userType(userType)
                .deleted(false)
                .llmGenerated(false)
                .build();
    }

    public static Mission createLlmGenerated(String title, String description, SpaceType spaceType,
                                             CategoryType categoryType, CompanionType companionType) {
        return Mission.builder()
                .title(title)
                .description(description)
                .spaceType(spaceType)
                .categoryType(categoryType)
                .companionType(companionType)
                .deleted(false)
                .llmGenerated(true)
                .build();
    }

    public void update(String title, String description, SpaceType spaceType,
                       CategoryType categoryType, CompanionType companionType,
                       EnjoyType enjoyType, UserType userType, boolean deleted) {
        this.title = title;
        this.description = description;
        this.spaceType = spaceType;
        this.categoryType = categoryType;
        this.companionType = companionType;
        this.enjoyType = enjoyType;
        this.userType = userType;
        this.deleted = deleted;
    }
}