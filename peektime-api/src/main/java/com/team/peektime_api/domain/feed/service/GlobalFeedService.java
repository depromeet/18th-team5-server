package com.team.peektime_api.domain.feed.service;

import com.team.peektime_api.domain.feed.cache.GlobalFeedCacheRepository;
import com.team.peektime_api.domain.feed.dto.FeedCacheItem;
import com.team.peektime_api.domain.feed.dto.FeedItemResponse;
import com.team.peektime_api.domain.feed.dto.GlobalFeedResponse;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.global.infra.S3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 전역 공유 피드 조회 서비스.
 *
 * 읽기 = Cache-aside(lazy loading): 캐시 확인 → MISS면 DB 조회 후 재적재.
 * 쓰기는 {@code GlobalFeedWriteThroughListener}가 담당(write-through) → "Cache-aside 읽기 + Write-through 쓰기" 하이브리드.
 *
 * 캐시 MISS(= 60초 주기 만료 시점)에 동시 요청이 몰리면 cache-stampede가 나므로, 재적재는 분산락 기반
 * single-flight로 한 요청만 수행한다. presigned URL은 캐시에 담지 않고 조회 시점에 생성한다(매 요청 만료되므로).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalFeedService {

    private static final int FEED_SIZE = 20;
    private static final String LOCK_KEY = "feed:recent:global:lock";
    private static final long LOCK_WAIT_SECONDS = 2;
    private static final long LOCK_LEASE_SECONDS = 3;

    private final UserMissionCompletionRepository completionRepository;
    private final GlobalFeedCacheRepository feedCacheRepository;
    private final RedissonClient redissonClient;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public GlobalFeedResponse getRecentFeed() {
        List<FeedCacheItem> items = feedCacheRepository.findRecent();
        if (items.isEmpty()) {
            items = loadWithSingleFlight(); // MISS → 한 요청만 DB 재적재
        }
        return toResponse(items);
    }

    /**
     * 캐시 MISS 시 single-flight: 락을 잡은 한 요청만 DB를 조회·재적재하고, 나머지는 잠깐 대기 후
     * 채워진 캐시를 읽는다(cache-stampede 차단). 락 획득 실패/Redis 장애 시에는 DB로 폴백해 조회 가용성을 보장한다.
     */
    private List<FeedCacheItem> loadWithSingleFlight() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (acquired) {
                // 더블 체크: 대기 동안 다른 요청이 이미 재적재했을 수 있음
                List<FeedCacheItem> cached = feedCacheRepository.findRecent();
                if (!cached.isEmpty()) {
                    return cached;
                }
                List<FeedCacheItem> fromDb = loadFromDb();
                feedCacheRepository.rebuild(fromDb);
                return fromDb;
            }
            // 락 못 잡음 → 채워졌으면 캐시, 아니면 DB 폴백
            List<FeedCacheItem> cached = feedCacheRepository.findRecent();
            return cached.isEmpty() ? loadFromDb() : cached;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loadFromDb();
        } catch (Exception e) {
            log.warn("피드 single-flight 처리 실패, DB 폴백: {}", e.getMessage());
            return loadFromDb();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
