package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.global.infra.cache.RecentRecordsCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecentRecordsCacheInvalidationEventListener {

    private final RecentRecordsCacheRepository recentRecordsCacheRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RecentRecordsCacheInvalidationEvent event) {
        try {
            recentRecordsCacheRepository.delete(event.getUserId());
            log.debug("최근 기록 캐시 삭제 완료: userId={}", event.getUserId());
        } catch (Exception e) {
            log.warn("최근 기록 캐시 삭제 실패 (userId={}): {}", event.getUserId(), e.getMessage());
        }
    }
}
