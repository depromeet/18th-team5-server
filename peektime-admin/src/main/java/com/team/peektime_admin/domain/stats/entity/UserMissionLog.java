package com.team.peektime_admin.domain.stats.entity;

import com.team.peektime_admin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    // 미션 완료 날짜. 일 단위 집계·보상은 로그 도착 시각(createdAt)이 아니라 이 값 기준 —
    // 자정 직전 완료 건이 재시도로 늦게 도착해도 실적 날짜가 어긋나지 않도록
    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Builder(access = AccessLevel.PRIVATE)
    private UserMissionLog(String idempotencyKey, Long userId, Long solarTermId, LocalDate completedDate) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.solarTermId = solarTermId;
        this.completedDate = completedDate;
    }

    public static UserMissionLog create(String idempotencyKey, Long userId, Long solarTermId, LocalDate completedDate) {
        return UserMissionLog.builder()
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .solarTermId(solarTermId)
                .completedDate(completedDate)
                .build();
    }
}