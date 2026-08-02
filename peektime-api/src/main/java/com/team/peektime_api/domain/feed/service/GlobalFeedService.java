package com.team.peektime_api.domain.feed.service;

import com.team.peektime_api.domain.feed.cache.GlobalFeedCacheRepository;
import com.team.peektime_api.domain.feed.dto.FeedCacheItem;
import com.team.peektime_api.domain.feed.dto.FeedItemResponse;
import com.team.peektime_api.domain.feed.dto.GlobalFeedResponse;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.infra.S3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 전역 공유 피드 조회 서비스.
 *
 * 캐시는 TTL 없이 상주하고(피드 20건이라 메모리 부담 없음), 쓰기는 {@code GlobalFeedWriteThroughListener}의
 * write-through, DB와의 동기화는 30초 주기 스케줄러의 {@link #syncCacheFromDb()}가 담당한다.
 * 주기 만료가 없으므로 읽기 경로에서 재적재 경쟁(cache-stampede)이 발생하지 않아 분산락(single-flight)이
 * 필요 없다 — 락 대기로 인한 조회 지연(p99 spike)을 제거하기 위한 전환이다.
 *
 * 캐시 MISS는 콜드 스타트/Redis 데이터 유실 시에만 발생하며, 이때는 DB 폴백 후 즉시 재적재해 워밍한다.
 * presigned URL은 캐시에 담지 않고 조회 시점에 생성한다(매 요청 만료되므로).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalFeedService {

    private static final int FEED_SIZE = 20;

    private final UserMissionCompletionRepository completionRepository;
    private final GlobalFeedCacheRepository feedCacheRepository;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public GlobalFeedResponse getRecentFeed() {
        List<FeedCacheItem> items = feedCacheRepository.findRecent();
        if (items.isEmpty()) {
            // 콜드 캐시(최초 기동/Redis 유실)에서만 도달 → DB 폴백 + 즉시 워밍.
            // 첫 재적재가 수 ms 내 끝나 이후 요청은 캐시 HIT이므로 락 없이도 쇄도가 지속되지 않는다.
            items = loadFromDb();
            feedCacheRepository.rebuild(items);
        }
        return toResponse(items);
    }

    /** DB 기준으로 캐시를 전체 동기화한다. (30초 주기 스케줄러에서 호출 — write-through 실패분 복구) */
    @Transactional(readOnly = true)
    public void syncCacheFromDb() {
        feedCacheRepository.rebuild(loadFromDb());
    }

    private List<FeedCacheItem> loadFromDb() {
        return completionRepository
                .findRecentGlobalWithImage(PageRequest.of(0, FEED_SIZE))
                .stream()
                .map(FeedCacheItem::from)
                .toList();
    }

    private GlobalFeedResponse toResponse(List<FeedCacheItem> items) {
        List<FeedItemResponse> responses = items.stream()
                .map(item -> FeedItemResponse.of(item, s3Service.generatePresignedViewUrl(item.objectKey())))
                .toList();
        return GlobalFeedResponse.of(responses);
    }
}
