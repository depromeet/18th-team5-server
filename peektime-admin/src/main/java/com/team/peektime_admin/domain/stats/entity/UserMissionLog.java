package com.team.peektime_admin.domain.stats.entity;

import com.team.peektime_admin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_mission_log", indexes = {
        @Index(name = "idx_solar_term_id", columnList = "solar_term_id"),
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

    @Column(name = "solar_term_id", nullable = false)
    private Long solarTermId;

    @Builder(access = AccessLevel.PRIVATE)
    private UserMissionLog(String idempotencyKey, String userUuid, Long solarTermId) {
        this.idempotencyKey = idempotencyKey;
        this.userUuid = userUuid;
        this.solarTermId = solarTermId;
    }

    public static UserMissionLog create(String idempotencyKey, String userUuid, Long solarTermId) {
        return UserMissionLog.builder()
                .idempotencyKey(idempotencyKey)
                .userUuid(userUuid)
                .solarTermId(solarTermId)
                .build();
    }
}