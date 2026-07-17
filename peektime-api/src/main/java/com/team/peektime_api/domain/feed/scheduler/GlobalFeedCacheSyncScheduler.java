package com.team.peektime_api.domain.feed.scheduler;

import com.team.peektime_api.domain.feed.service.GlobalFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 전역 피드 캐시 ↔ DB 동기화 스케줄러.
 *
 * 캐시가 TTL 없이 상주하므로, write-through 실패(dual-write)나 DB 측 변경(삭제 등)으로 생긴 불일치는
 * 만료로 자가 치유되지 않는다. 대신 30초 주기로 DB 기준 전체 재적재를 수행해 최대 30초 내 복구를 보장한다.
 * 상위 20건 조회 쿼리 1회/30초라 DB 부하는 무시할 수준이며, 실패 여부와 무관하게 무조건 동기화한다
 * (인스턴스별 실패 플래그 추적보다 단순하고, 이벤트 유실 등 추적 불가능한 불일치까지 복구).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalFeedCacheSyncScheduler {

    private final GlobalFeedService globalFeedService;

    @Scheduled(fixedDelay = 30_000)
    public void syncFeedCache() {
        try {
            globalFeedService.syncCacheFromDb();
        } catch (Exception e) {
            log.warn("전역 피드 캐시 동기화 실패 (다음 주기 재시도): {}", e.getMessage());
        }
    }
}
