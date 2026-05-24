package com.team.peektime_api.global.outbox;

/**
 * Outbox 이벤트 전송 결과
 * - Success: 전송 성공
 * - AlreadyProcessed: 이미 처리됨 (멱등성)
 * - Unknown: 결과 알 수 없음 (재시도 필요)
 */
public sealed interface SendResult {

    Long eventId();

    record Success(Long eventId) implements SendResult {}

    record AlreadyProcessed(Long eventId) implements SendResult {}

    record Unknown(Long eventId, String reason) implements SendResult {}
}