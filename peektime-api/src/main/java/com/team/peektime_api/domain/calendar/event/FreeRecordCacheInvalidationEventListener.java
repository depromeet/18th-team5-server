package com.team.peektime_api.domain.calendar.event;

import com.team.peektime_api.global.infra.cache.RecentRecordsCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 자유 기록 생성 이벤트를 받아 홈 최근 기록 캐시를 무효화하는 리스너
 *
 * - 미션 완료와 동일하게 캐시를 전체 삭제하여 다음 조회 시 DB에서 재구성
 * - 동기 실행 (캐시 삭제는 빠르므로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeRecordCacheInvalidationEventListener {

    private final RecentRecordsCacheRepository cacheRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFreeRecordCreated(FreeRecordCreatedEvent event) {
        Long userId = event.getUserId();
        try {
            cacheRepository.delete(userId);
            log.info("최근 기록 캐시 삭제 완료: userId={}, recordId={}", userId, event.getRecordId());
        } catch (Exception e) {
            log.warn("최근 기록 캐시 삭제 실패 (userId={}): {}", userId, e.getMessage());
        }
    }
}