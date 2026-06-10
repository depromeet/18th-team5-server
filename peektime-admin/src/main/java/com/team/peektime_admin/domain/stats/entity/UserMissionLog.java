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
        @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMissionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "solar_term_id", nullable = false)
    private Long solarTermId;

    @Builder(access = AccessLevel.PRIVATE)
    private UserMissionLog(String idempotencyKey, Long userId, Long solarTermId) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.solarTermId = solarTermId;
    }

    public static UserMissionLog create(String idempotencyKey, Long userId, Long solarTermId) {
        return UserMissionLog.builder()
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .solarTermId(solarTermId)
                .build();
    }
}