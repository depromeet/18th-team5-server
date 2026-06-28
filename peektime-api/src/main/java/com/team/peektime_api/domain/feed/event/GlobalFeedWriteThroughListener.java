package com.team.peektime_api.domain.feed.event;

import com.team.peektime_api.domain.feed.cache.GlobalFeedCacheRepository;
import com.team.peektime_api.domain.feed.dto.FeedCacheItem;
import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.event.MissionCompletedEvent;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 미션 완료 시 전역 피드 캐시를 write-through(직접 갱신)하는 리스너.
 *
 * - AFTER_COMMIT(D4): 커밋된 변경에 대해서만, 새 데이터가 보이는 시점에 갱신해 재적재 race를 막는다.
 * - 사진 없는 완료는 피드에서 제외(미션 인증 사진만 노출).
 * - Pull 방식: 이벤트의 completionId로 직접 조회.
 * - {@code addItem}은 TTL을 리셋하지 않고, 콜드 캐시면 추가하지 않는다(워밍된 캐시만 보강). 따라서 캐시 적재가
 *   실패하거나 콜드라 누락된 사진은 60초 주기 만료 후 다음 읽기의 DB 재적재로 최대 60초 내 복구된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalFeedWriteThroughListener {

    private final UserMissionCompletionRepository completionRepository;
    private final GlobalFeedCacheRepository feedCacheRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMissionCompleted(MissionCompletedEvent event) {
        completionRepository.findById(event.getCompletionId())
                .ifPresent(this::addToFeed);
    }

    private void addToFeed(UserMissionCompletion completion) {
        if (completion.getObjectKey() == null || completion.getObjectKey().isBlank()) {
            return; // 사진 없는 완료는 피드 제외
        }
        try {
            feedCacheRepository.addItem(FeedCacheItem.from(completion));
            log.info("전역 피드 write-through 완료: completionId={}", completion.getId());
        } catch (Exception e) {
            log.warn("전역 피드 write-through 실패 (completionId={}): {}", completion.getId(), e.getMessage());
        }
    }
}