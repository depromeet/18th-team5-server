package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.global.infra.cache.RecentRecordsCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 미션 완료 이벤트를 받아 캐시를 무효화하는 리스너
 *
 * - MissionCompletedEvent (팩트)를 받아서 캐시 삭제 (반응)
 * - 동기 실행 (캐시 삭제는 빠르므로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecentRecordsCacheInvalidationEventListener {

    private final RecentRecordsCacheRepository recentRecordsCacheRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMissionCompleted(MissionCompletedEvent event) {
        try {
            recentRecordsCacheRepository.delete(event.getUserId());
            log.debug("최근 기록 캐시 삭제 완료: userId={}", event.getUserId());
        } catch (Exception e) {
            log.warn("최근 기록 캐시 삭제 실패 (userId={}): {}", event.getUserId(), e.getMessage());
        }
    }
}