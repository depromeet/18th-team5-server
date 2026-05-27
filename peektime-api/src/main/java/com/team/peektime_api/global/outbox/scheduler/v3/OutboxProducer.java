package com.team.peektime_api.global.outbox.scheduler.v3;

import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Outbox 장전 담당 (Producer)
 *
 * - 분산락 + CountDownLatch로 Producer-Consumer 동기화
 * - Producer: DB에서 미발행 이벤트 조회 → Redis SET에 SADD
 * - 락 실패 인스턴스: Producer 완료까지 대기 후 Consumer로 참여
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProducer {

    private static final String OUTBOX_QUEUE_KEY = "outbox:set:queue";
    private static final String PRODUCER_LOCK_KEY = "outbox:producer:lock";
    private static final String PRODUCER_LATCH_KEY = "outbox:producer:latch";

    private final OutboxRepository outboxRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * Producer 역할 수행 또는 대기
     *
     * - 락 획득 성공: Producer로서 Redis SET 장전
     * - 락 획득 실패: Producer 완료까지 대기
     * - 모든 인스턴스가 동시에 Consumer 단계로 진입
     *
     * @return true: Producer로 장전 완료, false: 대기 후 진행
     */
    public boolean loadEventsToRedisWithSync() {
        RLock lock = redissonClient.getLock(PRODUCER_LOCK_KEY);
        RCountDownLatch latch = redissonClient.getCountDownLatch(PRODUCER_LATCH_KEY);

        boolean isProducer;
        try {
            isProducer = lock.tryLock(0, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[V3] Producer 락 획득 중 인터럽트");
            return false;
        }

        if (isProducer) {
            try {
                latch.trySetCount(1);
                doLoadEventsToRedis();
                latch.countDown();
                return true;
            } finally {
                lock.unlock();
            }
        } else {
            try {
                log.info("[V3] Producer 작업 완료 대기 중...");
                boolean awaited = latch.await(30, TimeUnit.SECONDS);
                if (!awaited) {
                    log.warn("[V3] Producer 대기 타임아웃");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[V3] Producer 대기 중 인터럽트");
            }
            return false;
        }
    }

    private void doLoadEventsToRedis() {
        List<Long> eventIds = outboxRepository.findAllIds();

        if (eventIds.isEmpty()) {
            log.info("[V3] 장전할 이벤트 없음");
            return;
        }

        String[] ids = eventIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        redisTemplate.opsForSet().add(OUTBOX_QUEUE_KEY, ids);
        log.info("[V3] Redis SET 장전 완료: {}건", eventIds.size());
    }
}