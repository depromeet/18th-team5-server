package com.team.peektime_api.global.outbox.scheduler.v5;

import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * V5 폴러/스위퍼의 트랜잭션 경계 — HTTP 전송 구간에는 트랜잭션도 락도 없다 (V4와 동일).
 *
 * V4와의 차이: claim이 리스 연장(next_retry_at 겸용)만 하는 대신 PROCESSING 전이 +
 * 전용 리스(lease_until)를 기록한다. "전송 중"이 상태로 가시화되고, 리스 만료 고아의
 * 복구는 claim 쿼리가 아니라 전용 스위퍼(OutboxLeaseRecoveryScheduler)의 단일 책임이 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxV5TransactionManager {

    private final OutboxRepository outboxRepository;

    // Tx1: READY & next_retry_at 도래분을 SKIP LOCKED로 잡고 PROCESSING + 리스 기록 후 즉시 커밋.
    // 커밋으로 row 락이 풀린 뒤의 재claim 방어는 상태+리스가 이어받는다 (OutboxEvent.claim 주석 참조).
    // Tx2가 크래시로 유실돼도 리스 만료 후 스위퍼가 READY로 되돌리므로 폴러는 복구를 모른다
    @Transactional
    public List<OutboxEvent> claimBatch() {
        List<OutboxEvent> events = outboxRepository.findClaimableWithSkipLocked(LocalDateTime.now());

        for (OutboxEvent event : events) {
            event.claim();
        }

        return events;
    }

    // Tx2: 전송 결과 반영 — PROCESSING을 SENT / FAILED / READY(백오프)로 닫는다 (리스도 함께 해제)
    @Transactional
    public void applyResults(List<SendResult> results) {
        List<Long> ids = results.stream().map(SendResult::eventId).toList();
        Map<Long, OutboxEvent> events = outboxRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(OutboxEvent::getId, Function.identity()));

        for (SendResult result : results) {
            OutboxEvent event = events.get(result.eventId());
            if (event == null) {
                continue;
            }

            if (result instanceof SendResult.Success) {
                event.markSent();

            } else if (result instanceof SendResult.PermanentFailure failure) {
                // 결정론적 결함 — 재시도해도 같은 결과라 백오프 없이 즉시 FAILED
                event.markFailed(failure.reason());
                log.error("[V5] Outbox 영구 실패 (FAILED, 운영자 확인 필요): id={}, reason={}",
                        event.getId(), failure.reason());

            } else if (result instanceof SendResult.RetryableFailure failure) {
                event.recordFailure(failure.reason());
                if (event.isFailed()) {
                    log.error("[V5] Outbox 재시도 상한 초과 (FAILED, 운영자 확인 필요): id={}, retryCount={}, reason={}",
                            event.getId(), event.getRetryCount(), failure.reason());
                } else {
                    log.warn("[V5] Outbox 전송 실패 (백오프 재시도): id={}, retryCount={}, nextRetryAt={}, reason={}",
                            event.getId(), event.getRetryCount(), event.getNextRetryAt(), failure.reason());
                }
            }
        }
    }

    // 리스너 즉시 전송 성공 시 호출
    @Transactional
    public void markSent(Long eventId) {
        outboxRepository.findById(eventId).ifPresent(OutboxEvent::markSent);
    }

    // 스위퍼 Tx: 리스 만료 PROCESSING을 READY로 복구 (복구 상한 초과분은 FAILED 승격).
    // 복구된 엔티티를 반환해 호출부가 건수·승격을 집계/로깅한다
    @Transactional
    public List<OutboxEvent> recoverExpiredLeases() {
        List<OutboxEvent> expired = outboxRepository.findExpiredProcessingWithSkipLocked(LocalDateTime.now());

        for (OutboxEvent event : expired) {
            event.recoverFromExpiredLease();
        }

        return expired;
    }
}