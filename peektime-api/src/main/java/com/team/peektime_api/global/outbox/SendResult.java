package com.team.peektime_api.global.outbox;

/**
 * Outbox 이벤트 전송 결과 — 성공/실패 2분류
 *
 * 일시/영구 실패를 구분하지 않는 이유:
 * - 이 도메인(통계 로그 적재)에는 비즈니스 거절 응답이 존재하지 않는다
 *   (Admin은 사실상 200 아니면 500만 응답)
 * - 수신 측이 멱등키로 dedup하므로 어떤 실패든 "재시도"가 안전한 유일 대응
 * - poison 건의 무한 재시도는 재시도 상한(초과 시 FAILED 승격)이 백스톱
 *
 * 단, 수신 측 멱등성이 전제다. 보상 지급처럼 멱등하지 않은 작업이 outbox를 타게 되면
 * "알 수 없음(전송 여부 불명)"을 1급 상태로 분리해야 한다.
 */
public sealed interface SendResult {

    Long eventId();

    record Success(Long eventId) implements SendResult {}

    record Failure(Long eventId, String reason) implements SendResult {}
}