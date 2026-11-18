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
        // 폴러 claim 쿼리(status='READY' AND next_retry_at <= ? ORDER BY next_retry_at LIMIT n)용.
        // 필터 동등 조건 → 정렬 컬럼 순의 복합 인덱스로 filesort 없는 스트리밍 플랜을 보장해
        // 잠기는 행을 반환되는 n건으로 최소화 (락킹 리드는 스캔 경로 전체에 락을 걸므로)
        @Index(name = "idx_outbox_status_next_retry", columnList = "status, next_retry_at"),
        // V5 스위퍼 쿼리(status='PROCESSING' AND lease_until < ? LIMIT n)용 — 같은 락 최소화 원리
        @Index(name = "idx_outbox_status_lease", columnList = "status, lease_until")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    // 리스너(즉시 전송)에게 주는 선공 유예 — 이 시간 안에는 폴러가 잡지 않는다
    private static final int LISTENER_GRACE_SECONDS = 3;

    // claim 리스: 정상 배치의 전송 구간 최대 시간(배치 30건 × 건당 타임아웃 5초(연결2+읽기3) = 150초)의
    // 2배 — 아직 전송 중인 row를 다른 폴러가 다시 잡는 중복 전송을 방지하면서, GC 멈춤·DB 지연·
    // 인스턴스 간 시계 오차 같은 계산 밖 변수를 마진으로 흡수한다.
    // 배치 크기나 RestClient 타임아웃을 바꾸면 이 값도 같이 재계산할 것
    private static final int CLAIM_LEASE_SECONDS = 300;

    // 재시도 상한: 백오프(5+10+15분) 기준 총 30분의 장애까지만 자동 복구로 커버.
    // 그보다 긴 장애는 FAILED 승격 → 알림 → 운영자 수동 재전송(UPDATE status='READY')이 담당한다.
    // 낮은 상한은 "FAILED 알림이 사람에게 즉시 닿는다"는 전제 위의 선택 — 알림 채널이 깨지면
    // 30분 이상의 장애가 조용히 수동 복구 대기 상태로 쌓이므로, 알림 인프라와 함께 유지보수할 것
    private static final int MAX_RETRY_COUNT = 3;

    // 백오프는 폴러 주기가 아니라 자동 복구 커버리지(5+10+15분 = 총 30분)에서 역산된 값 —
    // 상한을 낮추면 재배포 같은 수 분짜리 평범한 장애에도 FAILED가 쏟아지므로 상한(3회)과 세트로 조정할 것
    private static final int BACKOFF_BASE_MINUTES = 5;
    private static final int BACKOFF_MAX_MINUTES = 30;

    // V5 스위퍼의 크래시 루프 차단 상한: 같은 이벤트의 리스 만료 복구가 이 횟수를 넘으면
    // "전송 결과가 한 번도 Tx2에 기록되지 못하는 비정상"으로 간주해 FAILED 승격.
    // retryCount(전송이 실패로 응답한 횟수)와는 별개 축 — 크래시는 실패가 아니라 결과 불명이라
    // retryCount를 올리지 않으므로, 이 축이 없으면 크래시 루프가 조용히 무한 반복된다
    private static final int MAX_RECOVERY_COUNT = 3;

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

    // V5 처리 중 리스: claim 시 now+300초, 종결(SENT/FAILED)·복구 시 NULL.
    // "X초 지난 PROCESSING 회수"의 X를 스위퍼가 추정(V2의 updatedAt 임계)하는 대신
    // claim하는 쪽이 마감 시각으로 직접 적어둔다 — 기준 시각과 마진이 claim 한 곳에 모임.
    // V4가 next_retry_at에 겸용시키던 리스를 전용 컬럼으로 분리해
    // next_retry_at은 순수 백오프 예약으로 의미가 단일화된다
    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    // V5 리스 만료 복구 횟수 — 스위퍼만 증가시킨다 (MAX_RECOVERY_COUNT 주석 참조)
    @Column(name = "recovery_count", nullable = false)
    private int recoveryCount = 0;

    @Builder
    public OutboxEvent(String payload) {
        this.payload = payload;
        this.status = OutboxStatus.READY;
        this.retryCount = 0;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(LISTENER_GRACE_SECONDS);
    }

    // 리스 기반 claim: 상태 전이 없이 next_retry_at만 리스만큼 미뤄 전송 중 재claim을 막는다.
    // SKIP LOCKED의 행 락은 Tx1 커밋 순간 풀리므로 그 이후의 방어는 이 리스가 담당한다
    // (Tx1끼리 ms 단위로 겹치는 창은 SKIP LOCKED이 커버 — 둘이 빈틈없이 이어진다).
    // Tx2(결과 반영)가 크래시로 유실돼도 리스 만료 후 자동으로 다시 claim 대상이 되므로
    // 별도 복구 스케줄러가 필요 없다. 크래시는 실패가 아니라 결과 불명이므로 retryCount는
    // 건드리지 않는다 — 재전송 안전성은 Admin 멱등키 dedup이 보장
    public void extendLease() {
        this.nextRetryAt = LocalDateTime.now().plusSeconds(CLAIM_LEASE_SECONDS);
    }

    // V5 claim: 상태 전이(PROCESSING)와 리스 기록을 같은 트랜잭션에 묶는다 — 커밋으로
    // SKIP LOCKED의 row 락이 풀린 직후부터는 상태+리스가 재claim 방어를 이어받는다.
    // next_retry_at은 건드리지 않으므로, 일시 실패로 READY 복귀 시 백오프 예약이 그대로 산다
    public void claim() {
        this.status = OutboxStatus.PROCESSING;
        this.leaseUntil = LocalDateTime.now().plusSeconds(CLAIM_LEASE_SECONDS);
    }

    // V5 스위퍼 전용: 리스 만료된 PROCESSING을 READY로 되돌린다. 복구 발생 자체가
    // "크래시 또는 리스 초과가 있었다"는 이상 신호이므로 횟수를 남기고,
    // 상한 초과는 크래시 루프로 간주해 FAILED 승격 (MAX_RECOVERY_COUNT 주석 참조)
    public void recoverFromExpiredLease() {
        this.recoveryCount++;
        this.leaseUntil = null;

        if (this.recoveryCount > MAX_RECOVERY_COUNT) {
            this.status = OutboxStatus.FAILED;
            this.lastError = truncate(
                    "리스 만료 복구 상한(" + MAX_RECOVERY_COUNT + "회) 초과 — 크래시 루프 의심");
            return;
        }
        this.status = OutboxStatus.READY;
    }

    // 이하 두 개는 비활성 구버전(V1/V2) 폴러 전용 — V5의 PROCESSING 전이는 claim()이 리스와 함께 수행
    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void markReady() {
        this.status = OutboxStatus.READY;
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.leaseUntil = null;
    }

    // 영구 실패: 재시도 없이 즉시 FAILED. 요청 자체의 결정론적 결함(Admin 4xx, payload 파싱 불가)
    // 전용이며, 복구 경로는 상한 초과 FAILED와 동일 (알림 → 운영자 수정 후 수동 재전송)
    public void markFailed(String reason) {
        this.status = OutboxStatus.FAILED;
        this.lastError = truncate(reason);
        this.leaseUntil = null;
    }

    // 일시 실패 기록: 백오프 예약, 상한 초과 시 FAILED 승격
    public void recordFailure(String reason) {
        this.retryCount++;
        this.lastError = truncate(reason);
        this.leaseUntil = null;

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
