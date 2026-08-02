package com.team.peektime_api.global.outbox.entity;

/**
 * Outbox 이벤트 상태 머신
 *
 *   READY ──▶ PROCESSING ──▶ SENT    (성공. 하드 딜리트 대신 마킹 — 대사(reconciliation) 대비 보존)
 *     ▲            │
 *     │            └────────▶ FAILED (영구 실패 / 재시도 상한 초과. payload 보존, 운영자 개입 대상)
 *     └── 백오프 재시도 / stuck 복구
 *
 * - READY: 폴러 claim 대상 (단, next_retry_at 도래분만)
 * - PROCESSING: 폴러가 잡아감. 오래 방치되면 복구 스케줄러가 READY로 되돌림
 * - SENT: 전송 완료. 어떤 스케줄러도 읽지 않음 (추후 보존 기한 지난 건 purge 예정)
 * - FAILED: 자동 재시도 정지. 수동 복구는 UPDATE status='READY'
 */
public enum OutboxStatus {
    READY,
    PROCESSING,
    SENT,
    FAILED
}