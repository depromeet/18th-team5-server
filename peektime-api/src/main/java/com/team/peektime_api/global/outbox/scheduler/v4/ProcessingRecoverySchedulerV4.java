package com.team.peektime_api.global.outbox.scheduler.v4;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * stuck PROCESSING 복구 스케줄러 (V4)
 *
 * PROCESSING인 채 방치되는 경로 세 가지 — 폴러는 READY만 보므로 별도 복구가 없으면 영원히 갇힌다:
 * 1. Tx2 롤백 (전송은 됐는데 결과 반영 실패)
 * 2. Tx1 커밋 직후 서버 다운
 * 3. 전송 중 서버 다운
 *
 * threshold 600초 근거: row가 PROCESSING으로 머무는 정상 최대 시간은
 * 자기 배치의 전송 구간 = 배치 30건 × 건당 최대 10초(연결 5 + 읽기 5) = 300초.
 * 그 두 배로 잡아 아직 전송 중인 row를 stuck으로 오판하는 일을 방지한다.
 * 배치 크기나 RestClient 타임아웃을 바꾸면 이 값도 같이 재계산해야 한다.
 *
 * 복구는 중복 전송을 감수하는 동작이다 (케이스 1·3은 이미 Admin에 저장됐을 수 있음).
 * Admin 멱등키 dedup이 있어 데이터는 안전하다.
 * stuck은 실패가 아니라 결과 불명이므로 retryCount는 건드리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessingRecoverySchedulerV4 {

    private static final int STUCK_THRESHOLD_SECONDS = 600;

    private final OutboxV4TransactionManager transactionManager;

    @Scheduled(fixedDelay = 300_000)
    public void recoverStuckProcessing() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(STUCK_THRESHOLD_SECONDS);

        int recoveredCount = transactionManager.recoverStuckProcessing(threshold);

        if (recoveredCount > 0) {
            log.warn("[V4 Recovery] stuck PROCESSING 복구: {}건", recoveredCount);
        }
    }
}