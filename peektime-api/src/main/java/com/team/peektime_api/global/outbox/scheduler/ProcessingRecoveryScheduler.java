package com.team.peektime_api.global.outbox.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Stuck PROCESSING 복구 스케줄러
 *
 * 문제 상황:
 * - Tx1에서 PROCESSING으로 전환 후 외부 API 실패
 * - 외부 API 성공했지만 Tx2에서 삭제 실패
 * → 둘 다 PROCESSING 상태로 남음
 *
 * 해결:
 * - updatedAt이 90초 이상 지난 PROCESSING → READY로 복구
 * - 100초마다 실행 (정상 처리 최대 90초 + 여유 10초)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessingRecoveryScheduler {

    private static final int STUCK_THRESHOLD_SECONDS = 90;

    private final OutboxTransactionManager transactionManager;

    // @Scheduled(fixedDelay = 100_000)
    public void recoverStuckProcessing() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(STUCK_THRESHOLD_SECONDS);

        int recoveredCount = transactionManager.recoverStuckProcessing(threshold);

        if (recoveredCount > 0) {
            log.warn("[Recovery] stuck PROCESSING 복구 완료: {}건", recoveredCount);
        } else {
            log.info("[Recovery] stuck PROCESSING 없음");
        }
    }
}