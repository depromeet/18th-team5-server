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

    // 재시도 상한: 백오프(5+10+15분) 기준 총 30분의 장애까지만 자동 복구로 커버.
    // 그보다 긴 장애는 FAILED 승격 → 알림 → 운영자 수동 재전송(UPDATE status='READY')이 담당한다.
    // 낮은 상한은 "FAILED 알림이 사람에게 즉시 닿는다"는 전제 위의 선택 — 알림 채널이 깨지면
    // 30분 이상의 장애가 조용히 수동 복구 대기 상태로 쌓이므로, 알림 인프라와 함께 유지보수할 것
    private static final int MAX_RETRY_COUNT = 3;

    // 백오프는 폴러 주기가 아니라 자동 복구 커버리지(5+10+15분 = 총 30분)에서 역산된 값 —
    // 상한을 낮추면 재배포 같은 수 분짜리 평범한 장애에도 FAILED가 쏟아지므로 상한(3회)과 세트로 조정할 것
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

    // 전송 실패 기록: 백오프 예약, 상한 초과 시 FAILED 승격
    public void recordFailure(String reason) {
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
