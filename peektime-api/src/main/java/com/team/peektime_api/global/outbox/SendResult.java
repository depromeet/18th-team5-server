package com.team.peektime_api.global.outbox;

/**
 * Outbox 이벤트 전송 결과 — 성공 / 일시 실패(재시도) / 영구 실패(즉시 FAILED) 3분류
 *
 * 분류 계약 (Admin과의 합의, 어기면 데이터 유실로 이어진다):
 * - Admin은 요청 내용만으로 결정론적으로 판정되는 결함(JSON 파싱 불가, 필수 필드 누락,
 *   타입 오류)에만 4xx를 응답한다. DB 상태·부하 등 일시적일 수 있는 문제에 4xx 금지 —
 *   4xx는 재시도 없이 즉시 FAILED로 가므로, 오분류 시 복구 가능한 건이 재시도 없이 죽는다.
 * - 새 필수 필드 추가는 2단계 배포로 간다: Admin이 먼저 "없으면 관용 처리"로 배포한 뒤
 *   API가 필드를 보내기 시작하고, 한참 뒤에야 required로 승격한다. 이 규칙을 어기면
 *   배포 전환기 outbox 잔여 건이 전부 즉시 FAILED가 된다 (completedDate 전환 사례 참조).
 *
 * PermanentFailure는 재시도해봐야 같은 결과가 보장되는 경우 전용:
 * - Admin 4xx 응답 (위 계약에 따라 결정론적 결함만)
 * - 전송 전 payload 파싱 실패 (로컬에서 결정론적)
 *
 * 타임아웃·커넥션 오류처럼 전송 여부 불명인 경우는 반드시 RetryableFailure다 —
 * 수신 측 멱등키 dedup이 재시도를 안전하게 만든다. 보상 지급처럼 멱등하지 않은 작업이
 * outbox를 타게 되면 "알 수 없음(전송 여부 불명)"을 1급 상태로 분리해야 한다.
 */
public sealed interface SendResult {

    Long eventId();

    record Success(Long eventId) implements SendResult {}

    // 재시도 대상: 5xx, 타임아웃, 커넥션 오류 등 — 백오프 후 폴러가 재시도, 상한 초과 시 FAILED
    record RetryableFailure(Long eventId, String reason) implements SendResult {}

    // 재시도 무의미: 요청 자체의 결정론적 결함 — 백오프를 태우지 않고 즉시 FAILED + 알림
    record PermanentFailure(Long eventId, String reason) implements SendResult {}
}