package com.team.peektime_api.global.outbox.entity;

/**
 * Outbox 이벤트 상태 머신 (V5: PROCESSING 가시화 + 리스 복구 스위퍼)
 *
 *   READY ──claim(폴러 Tx1)──▶ PROCESSING ──성공──▶ SENT
 *     ▲                            │
 *     │                            ├─ 일시 실패 ────▶ READY (next_retry_at = 백오프)
 *     │                            ├─ 영구 실패/재시도 상한 ──▶ FAILED
 *     │                            │
 *     └── 스위퍼: 리스(lease_until) 만료 복구 ──┘ (복구 상한 초과 시 크래시 루프로 보고 FAILED)
 *
 * - READY: 폴러 claim 대상 (단, next_retry_at 도래분만 — 백오프 대기 포함)
 * - PROCESSING: claim됨(전송 중). lease_until이 소유권 만료 시각이며, 만료분의 수거는
 *   claim 쿼리가 아니라 전용 스위퍼(OutboxLeaseRecoveryScheduler)의 단일 책임 —
 *   복구 발생이 건수/로그로 관측되게 하기 위한 분리 (정상 운영에서 복구는 0건이어야 한다)
 * - SENT: 전송 완료. 어떤 스케줄러도 읽지 않음 (추후 보존 기한 지난 건 purge 예정)
 * - FAILED: 자동 재시도 정지. 수동 복구는 UPDATE status='READY'
 */
public enum OutboxStatus {
    READY,
    PROCESSING,
    SENT,
    FAILED
}