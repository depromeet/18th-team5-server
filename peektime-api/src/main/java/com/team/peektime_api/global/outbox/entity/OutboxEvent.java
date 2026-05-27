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
        @Index(name = "idx_outbox_status", columnList = "status")
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