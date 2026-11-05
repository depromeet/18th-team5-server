package com.team.peektime_api.domain.feed.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.team.peektime_api.domain.feed.dto.FeedCacheItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * 전역 공유 피드의 인스턴스 로컬 캐시 (Caffeine, 단일 키).
 *
 * <p>피드는 최대 20건의 전역 단일 목록이라 캐시 키가 하나뿐이다. 신선도는 10초 주기 스케줄러가
 * {@link #put}으로 DB 스냅샷을 밀어 넣어(시계 트리거) 보장한다 — 트래픽과 무관하게 ≤10초 신선하고,
 * 완료 직후 본인 글도 다음 틱에 반드시 노출된다. 쓰기 경로의 write-through는 두지 않는다:
 * 캐시에 쓰는 주체가 스케줄러 하나뿐이라 dual-write race가 구조적으로 존재하지 않는다.
 *
 * <p>로컬 캐시에 Caffeine을 쓰는 이유는 콜드 스타트(최초 기동, 첫 틱 이전)의 동시 적재를
 * {@link #getOrLoad}의 로더가 single-flight로 1회만 실행하도록 묶어 주고, 프로젝트의 로컬 캐시
 * 표준을 통일하기 위함이다. 교체는 참조 대입이라 원자적이며, 읽는 쪽은 항상 이전 스냅샷 전체 또는
 * 새 스냅샷 전체만 본다(부분 노출 없음).
 *
 * <p>다중 인스턴스에서는 각 인스턴스가 자기 메모리 캐시를 독립적으로 채운다(스케줄러에 분산락 미적용).
 * 완료 커밋은 모든 인스턴스가 공유하는 DB에 반영되므로, 어느 인스턴스로 조회해도 다음 틱(≤10초)에 수렴한다.
 */
@Component
public class GlobalFeedLocalCache {

    private static final String KEY = "global";

    private final Cache<String, List<FeedCacheItem>> cache = Caffeine.newBuilder().build();

    /**
     * 캐시에 있으면 반환, 콜드(미적재)면 로더로 적재한다.
     * 동시 콜드 접근은 Caffeine이 single-flight로 묶어 로더(DB 조회)를 1회만 실행한다.
     */
    public List<FeedCacheItem> getOrLoad(Supplier<List<FeedCacheItem>> loader) {
        return cache.get(KEY, k -> loader.get());
    }

    /** DB 스냅샷으로 캐시를 통째 교체한다(원자적 참조 대입). 10초 주기 스케줄러가 호출. */
    public void put(List<FeedCacheItem> items) {
        cache.put(KEY, List.copyOf(items));
    }
}