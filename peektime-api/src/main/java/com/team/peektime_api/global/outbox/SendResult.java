package com.team.peektime_api.global.outbox;

/**
 * Outbox 이벤트 전송 결과
 *
 * - Success: 전송 성공 (2xx) → 삭제
 * - PermanentFailure: 영구 실패 (4xx, 파싱 에러 등) → 삭제 (재시도 무의미)
 * - TransientFailure: 일시 실패 (5xx, 타임아웃, 네트워크) → 유지 (재시도 필요)
 */
public sealed interface SendResult {

    Long eventId();

    record Success(Long eventId) implements SendResult {}

    record PermanentFailure(Long eventId, String reason) implements SendResult {}

    record TransientFailure(Long eventId, String reason) implements SendResult {}
}