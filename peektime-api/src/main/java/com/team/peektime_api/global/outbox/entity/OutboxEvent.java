package com.team.peektime_api.global.outbox.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_created_at", columnList = "created_at"),
        // V4 폴러 claim 쿼리(status='READY' AND next_retry_at <= ? ORDER BY next_retry_at LIMIT n)용.
        // 필터 동등 조건 → 정렬 컬럼 순의 복합 인덱스로 filesort 없는 스트리밍 플랜을 보장해
        // 잠기는 행을 반환되는 n건으로 최소화 (락킹 리드는 스캔 경로 전체에 락을 걸므로)
        @Index(name = "idx_outbox_status_next_retry", columnList = "status, next_retry_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    // 리스너(즉시 전송)에게 주는 선공 유예 — 이 시간 안에는 폴러가 잡지 않는다
    private static final int LISTENER_GRACE_SECONDS = 3;

    // 재시도 상한: 백오프 스케줄(5분 × n, 상한 30분) 기준 약 5시간의 장애를 자동 복구로 커버.
    // 초과는 자동 재시도가 무의미하다고 보고 FAILED로 승격해 운영자를 부른다
    private static final int MAX_RETRY_COUNT = 15;

    // 폴러 주기(5분)보다 짧은 백오프는 의미가 없으므로 주기를 베이스로 선형 증가
    private static final int BACKOFF_BASE_MINUTES = 5;
    private static final int BACKOFF_MAX_MINUTES = 30;

    private static final int LAST_ERROR_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.READY;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    // 이 시각이 되기 전에는 폴러 claim 대상에서 제외된다
    // (신규: 리스너 선공 유예, 일시 실패: 백오프 예약 시각)
    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder
    public OutboxEvent(String payload) {
        this.payload = payload;
        this.status = OutboxStatus.READY;
        this.retryCount = 0;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(LISTENER_GRACE_SECONDS);
    }

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    // stuck PROCESSING 복구용. stuck은 실패가 아니라 결과 불명이므로 retryCount를 깎지 않고,
    // nextRetryAt도 갱신하지 않는다 (이미 지난 시각이라 다음 폴링에서 바로 잡힘)
    public void markReady() {
        this.status = OutboxStatus.READY;
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = OutboxStatus.FAILED;
        this.lastError = truncate(reason);
    }

    // 일시 실패 기록: 백오프 예약, 상한 초과 시 FAILED 승격
    public void recordTransientFailure(String reason) {
        this.retryCount++;
        this.lastError = truncate(reason);

        if (this.retryCount > MAX_RETRY_COUNT) {
            this.status = OutboxStatus.FAILED;
            return;
        }

        this.status = OutboxStatus.READY;
        int backoffMinutes = Math.min(BACKOFF_BASE_MINUTES * this.retryCount, BACKOFF_MAX_MINUTES);
        this.nextRetryAt = LocalDateTime.now().plusMinutes(backoffMinutes);
    }

    public boolean isFailed() {
        return this.status == OutboxStatus.FAILED;
    }

    private static String truncate(String reason) {
        if (reason == null || reason.length() <= LAST_ERROR_MAX_LENGTH) {
            return reason;
        }
        return reason.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
