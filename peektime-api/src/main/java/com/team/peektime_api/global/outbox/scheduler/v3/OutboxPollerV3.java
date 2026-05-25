package com.team.peektime_api.global.outbox.scheduler.v3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * OutboxPoller V3 - Redis SET 기반
 *
 * 흐름:
 * 1. [장전] 분산락으로 하나의 인스턴스만 DB → Redis SET 장전
 * 2. [소비] 모든 인스턴스가 SPOP으로 병렬 처리
 *
 * V2와의 차이점:
 * - V2: DB 상태(PROCESSING) 기반, @Transactional 필요
 * - V3: Redis SET 기반, @Transactional 불필요
 *
 * 장점:
 * - 트랜잭션 복잡도 감소
 * - 인스턴스 개수와 무관하게 자동 분배
 * - DB 락 불필요
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollerV3 {

    private final OutboxProducer producer;
    private final OutboxConsumer consumer;

    // @Scheduled(fixedDelay = 60_000)
    public void poll() {
        log.info("[V3] Outbox 폴링 시작");

        // 1. 장전 (락 잡은 인스턴스만 실행)
        producer.loadEventsToRedis();

        // 2. 소비 (모든 인스턴스가 병렬 처리)
        consumer.consumeAll();

        log.info("[V3] Outbox 폴링 완료");
    }
}


/**
 * Set 자료구조를 사용한 이유:
 * -> 어떤 특정 스케줄링이 Producer 의 인스턴스가 Lock이 풀리고 실행이 되어
 * Lock 을 소유하고 Outbox 테이블을 조회후에 Set 에 insert 를 할것이다.
 * DB delete 를 하기전, 그리고 스케줄링이 의도치 않게, Lock이 풀리고 실행이됨.
 */