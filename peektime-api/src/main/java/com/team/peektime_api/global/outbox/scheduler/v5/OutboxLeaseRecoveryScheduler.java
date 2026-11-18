package com.team.peektime_api.global.outbox.scheduler.v5;

import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리스 만료 PROCESSING 복구 스위퍼 (V5)
 *
 * 폴러가 claim(PROCESSING + lease_until) 후 Tx2에 도달하지 못하면(크래시, 리스 초과)
 * 행이 PROCESSING에 남는다. 이 스위퍼가 리스 만료분을 READY로 되돌려 폴러가 재전송하게 한다.
 * 재전송의 안전성은 Admin 멱등키 dedup이 보장 (크래시는 실패가 아니라 결과 불명이므로
 * retry_count는 건드리지 않는다).
 *
 * 복구를 claim 쿼리에 접지 않고 전용 스위퍼로 분리한 이유 — 관측:
 * 정상 운영에서 복구는 0건이어야 하므로 복구 발생 자체가 이상 신호다. WARN 로그가 곧 알림 소스가
 * 되고, 같은 이벤트의 복구가 상한(3회)을 넘으면 전송 결과가 한 번도 기록되지 못하는 크래시 루프로
 * 간주해 FAILED 승격한다 (OutboxEvent.recoverFromExpiredLease). 이 "복구 횟수" 축은
 * 접이식 claim에는 셀 자리가 없던 것이다.
 *
 * 주기 60초: 복구 지연은 최대 "리스 300초 + 60초". 다중 인스턴스 동시 실행은 SKIP LOCKED가
 * 분배하므로 @SchedulerLock이 필요 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxLeaseRecoveryScheduler {

    private final OutboxV5TransactionManager transactionManager;

    @Scheduled(fixedDelay = 60_000)
    public void recoverExpiredLeases() {
        List<OutboxEvent> recovered = transactionManager.recoverExpiredLeases();
        if (recovered.isEmpty()) {
            return;
        }

        List<Long> promotedIds = recovered.stream()
                .filter(OutboxEvent::isFailed)
                .map(OutboxEvent::getId)
                .toList();

        log.warn("[V5-Recovery] 리스 만료 PROCESSING 복구: {}건, ids={} (정상 운영에서는 0건이어야 함 — 크래시/리스 초과 발생 신호)",
                recovered.size(), recovered.stream().map(OutboxEvent::getId).toList());

        if (!promotedIds.isEmpty()) {
            log.error("[V5-Recovery] 복구 상한 초과로 FAILED 승격 (크래시 루프 의심, 운영자 확인 필요): ids={}",
                    promotedIds);
        }
    }
}