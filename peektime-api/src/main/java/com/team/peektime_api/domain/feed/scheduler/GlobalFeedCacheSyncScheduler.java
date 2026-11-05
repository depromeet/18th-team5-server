package com.team.peektime_api.domain.feed.scheduler;

import com.team.peektime_api.domain.feed.service.GlobalFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 전역 피드 캐시 ↔ DB 동기화 스케줄러 (신선도의 유일한 트리거).
 *
 * 로컬 캐시에 write-through를 두지 않으므로, 새 완료가 캐시에 반영되는 경로는 이 주기 동기화뿐이다.
 * 5초마다 DB 기준 상위 20건 스냅샷으로 캐시를 통째 교체해, 트래픽과 무관하게 ≤5초 신선도를
 * (완료 직후 본인 글 포함) 무조건 보장한다("5초 지연 허용" 요구사항에 맞춘 주기).
 *
 * fixedDelay이므로 실제 주기는 "직전 실행 종료 + 5초"다. 신선도 상한은 엄밀히 5초 + 동기화 1회 소요지만,
 * 조건부 재캐싱({@link GlobalFeedService#syncCacheFromDb()})으로 새 완료가 없으면 가벼운 MAX 조회만
 * 하고 상위 20건 교체는 스킵하므로, 조용한 시간대의 소요는 무시할 수준이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalFeedCacheSyncScheduler {

    private final GlobalFeedService globalFeedService;

    // @SchedulerLock 의도적 미적용 — 인스턴스별 로컬 메모리 캐시를 채우는 작업이라
    // 모든 인스턴스가 각자 돌아야 한다. 플릿 1회로 제한하면 락을 못 잡은 인스턴스의 캐시가 안 채워진다
    @Scheduled(fixedDelay = 5_000)
    public void syncFeedCache() {
        try {
            globalFeedService.syncCacheFromDb();
        } catch (Exception e) {
            log.warn("전역 피드 캐시 동기화 실패 (다음 주기 재시도): {}", e.getMessage());
        }
    }
}
