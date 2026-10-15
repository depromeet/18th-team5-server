package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.MissionType;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_mission_completion", indexes = {
        @Index(name = "idx_completion_user_term", columnList = "user_id, solar_term_id"),
        @Index(name = "idx_completion_user_term_date", columnList = "user_id, solar_term_id, created_at")
}, uniqueConstraints = {
        // 멱등성 키(userId:missionId:completedDate)와 같은 구조 — 같은 날 같은 미션 중복 완료를
        // 애플리케이션 체크가 아닌 DB 제약으로 보장 (동시 요청 레이스 백스톱)
        @UniqueConstraint(name = "uk_completion_user_mission_date",
                columnNames = {"user_id", "mission_id", "completed_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMissionCompletion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solar_term_id", nullable = false)
    private SolarTerm solarTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "memo", length = 200)
    private String memo;

    // 미션을 완료한 날짜 — created_at(저장 시각)과 달리 비즈니스 기준일.
    // 유니크 제약의 구성 요소라 not null
    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Builder(access = AccessLevel.PRIVATE)
    private UserMissionCompletion(User user, Mission mission, SolarTerm solarTerm,
                                  MissionType missionType, String objectKey, String memo,
                                  LocalDate completedDate) {
        this.user = user;
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.missionType = missionType;
        this.objectKey = objectKey;
        this.memo = memo;
        this.completedDate = completedDate;
    }

    public void update(String objectKey, String memo) {
        this.objectKey = objectKey;
        this.memo = memo;
    }

    public static UserMissionCompletion create(User user, Mission mission, SolarTerm solarTerm,
                                               MissionType missionType, String objectKey, String memo,
                                               LocalDate completedDate) {
        return UserMissionCompletion.builder()
                .user(user)
                .mission(mission)
                .solarTerm(solarTerm)
                .missionType(missionType)
                .objectKey(objectKey)
                .memo(memo)
                .completedDate(completedDate)
                .build();
    }
}
