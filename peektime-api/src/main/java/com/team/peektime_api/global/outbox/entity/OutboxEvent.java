package com.team.peektime_api.global.outbox.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_created_at", columnList = "created_at"),
        // 폴러 claim 쿼리(status='READY' AND created_at < ? ORDER BY created_at LIMIT n)용.
        // 필터 동등 조건 → 정렬 컬럼 순의 복합 인덱스로 filesort 없는 스트리밍 플랜을 보장해
        // 잠기는 행을 반환되는 n건으로 최소화 (락킹 리드는 스캔 경로 전체에 락을 걸므로)
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.READY;

    @Builder
    public OutboxEvent(String payload) {
        this.payload = payload;
        this.status = OutboxStatus.READY;
    }

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void markReady() {
        this.status = OutboxStatus.READY;
    }
}