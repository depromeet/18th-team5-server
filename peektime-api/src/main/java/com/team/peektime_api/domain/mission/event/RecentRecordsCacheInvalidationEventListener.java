package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.infra.cache.RecentRecordsCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 미션 완료 이벤트를 받아 캐시를 무효화하는 리스너
 *
 * - Pull 방식: completionId로 필요한 데이터 직접 조회
 * - 동기 실행 (캐시 삭제는 빠르므로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecentRecordsCacheInvalidationEventListener {

    private final UserMissionCompletionRepository completionRepository;
    private final RecentRecordsCacheRepository cacheRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMissionCompleted(MissionCompletedEvent event) {
        completionRepository.findById(event.getCompletionId())
                .ifPresentOrElse(
                        this::invalidateCache,
                        () -> log.warn("캐시 삭제 스킵 - completion 없음: id={}", event.getCompletionId())
                );
    }

    private void invalidateCache(UserMissionCompletion completion) {
        Long userId = completion.getUser().getId();
        try {
            cacheRepository.delete(userId);
            log.debug("최근 기록 캐시 삭제 완료: userId={}", userId);
        } catch (Exception e) {
            log.warn("최근 기록 캐시 삭제 실패 (userId={}): {}", userId, e.getMessage());
        }
    }
}
