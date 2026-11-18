package com.team.peektime_api.global.outbox.scheduler.v4;

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
 * V4 폴러의 트랜잭션 경계 — HTTP 전송 구간에는 트랜잭션도 락도 없다.
 *
 * Tx1(claim)과 Tx2(결과 반영)는 각각 수 ms짜리 짧은 트랜잭션이고,
 * 그 사이의 외부 API 호출은 커넥션을 점유하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxV4TransactionManager {

    private final OutboxRepository outboxRepository;

    // Tx1: READY & next_retry_at 도래분을 SKIP LOCKED로 잡고 리스(next_retry_at 미룸)만 걸고 즉시 커밋.
    // 상태 전이 없음 — 커밋으로 락이 풀린 뒤의 재claim 방어는 리스가, Tx1끼리 겹치는 ms 창은
    // SKIP LOCKED이 담당한다. 크래시로 Tx2가 유실돼도 리스 만료 후 자동 재claim되므로
    // 복구 스케줄러가 필요 없다 (OutboxEvent.extendLease 주석 참조)
    @Transactional
    public List<OutboxEvent> claimBatch() {
        List<OutboxEvent> events = outboxRepository.findClaimableWithSkipLocked(LocalDateTime.now());

        for (OutboxEvent event : events) {
            event.extendLease();
        }

        return events;
    }

    // Tx2: 전송 결과 반영 — 성공은 SENT 마킹(하드 딜리트 대신 보존), 실패는 백오프 또는 FAILED
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
                log.error("[V4] Outbox 영구 실패 (FAILED, 운영자 확인 필요): id={}, reason={}",
                        event.getId(), failure.reason());

            } else if (result instanceof SendResult.RetryableFailure failure) {
                event.recordFailure(failure.reason());
                if (event.isFailed()) {
                    log.error("[V4] Outbox 재시도 상한 초과 (FAILED, 운영자 확인 필요): id={}, retryCount={}, reason={}",
                            event.getId(), event.getRetryCount(), failure.reason());
                } else {
                    log.warn("[V4] Outbox 전송 실패 (백오프 재시도): id={}, retryCount={}, nextRetryAt={}, reason={}",
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
}
