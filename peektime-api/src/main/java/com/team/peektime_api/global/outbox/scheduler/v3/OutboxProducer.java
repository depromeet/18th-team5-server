package com.team.peektime_api.global.outbox.scheduler.v3;

import com.team.peektime_api.global.aop.DistributedLock;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 장전 담당 (Producer)
 *
 * - 분산락으로 하나의 인스턴스만 실행
 * - DB에서 미발행 이벤트 조회 → Redis SET에 SADD
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProducer {

    private static final String OUTBOX_QUEUE_KEY = "outbox:set:queue";

    private final OutboxRepository outboxRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @DistributedLock(key = "'outbox:producer'", waitTime = 0, leaseTime = 10)
    public boolean loadEventsToRedis() {
        List<Long> eventIds = outboxRepository.findAllIds();

        if (eventIds.isEmpty()) {
            log.info("[V3] 장전할 이벤트 없음");
            return true;
        }

        String[] ids = eventIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        redisTemplate.opsForSet().add(OUTBOX_QUEUE_KEY, ids);
        log.info("[V3] Redis SET 장전 완료: {}건", eventIds.size());

        return true;
    }
}