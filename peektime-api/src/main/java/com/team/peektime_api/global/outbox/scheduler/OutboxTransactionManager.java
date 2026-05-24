package com.team.peektime_api.global.outbox.scheduler;

import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxTransactionManager {

    private final OutboxRepository outboxRepository;

    @Transactional
    public List<OutboxEvent> fetchAndMarkProcessing() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
        List<OutboxEvent> events = outboxRepository.findReadyEventsWithSkipLocked(threshold);

        for (OutboxEvent event : events) {
            event.markProcessing();
        }

        return events;
    }

    @Transactional
    public void handleResults(List<Long> toDelete, List<Long> toRetry) {
        if (!toDelete.isEmpty()) {
            outboxRepository.deleteAllByIdInBatch(toDelete);
            log.info("[V2] Outbox 삭제 완료: {}건", toDelete.size());
        }

        if (!toRetry.isEmpty()) {
            List<OutboxEvent> retryEvents = outboxRepository.findAllById(toRetry);
            for (OutboxEvent event : retryEvents) {
                event.markReady();
            }
            log.warn("[V2] Outbox 재시도 대기: {}건", toRetry.size());
        }
    }
}