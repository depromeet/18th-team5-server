package com.team.peektime_admin.domain.stats.entity;

import com.team.peektime_admin.global.common.BaseEntity;
import com.team.peektime_admin.global.common.enums.MissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_mission_log", indexes = {
        @Index(name = "idx_mission_id", columnList = "mission_id"),
        @Index(name = "idx_solar_term_id", columnList = "solar_term_id"),
        @Index(name = "idx_completed_date", columnList = "completed_date"),
        @Index(name = "idx_user_uuid", columnList = "user_uuid")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMissionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userUuid;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Column(name = "solar_term_id", nullable = false)
    private Long solarTermId;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserMissionLog(String idempotencyKey, String userUuid, Long missionId, MissionType missionType,
                           Long solarTermId, LocalDate completedDate, LocalDateTime completedAt) {
        this.idempotencyKey = idempotencyKey;
        this.userUuid = userUuid;
        this.missionId = missionId;
        this.missionType = missionType;
        this.solarTermId = solarTermId;
        this.completedDate = completedDate;
        this.completedAt = completedAt != null ? completedAt : LocalDateTime.now();
    }

    public static UserMissionLog create(String idempotencyKey, String userUuid, Long missionId,
                                         MissionType missionType, Long solarTermId,
                                         LocalDate completedDate, LocalDateTime completedAt) {
        return UserMissionLog.builder()
                .idempotencyKey(idempotencyKey)
                .userUuid(userUuid)
                .missionId(missionId)
                .missionType(missionType)
                .solarTermId(solarTermId)
                .completedDate(completedDate)
                .completedAt(completedAt)
                .build();
    }
}