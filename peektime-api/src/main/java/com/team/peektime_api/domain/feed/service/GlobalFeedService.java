package com.team.peektime_api.domain.feed.service;

import com.team.peektime_api.domain.feed.cache.GlobalFeedLocalCache;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 전역 공유 피드 조회 서비스.
 *
 * 피드는 인스턴스 로컬 캐시(Caffeine, 단일 키)에 상주한다(최대 20건이라 메모리 부담 없음).
 * 신선도는 5초 주기 스케줄러의 {@link #syncCacheFromDb()}가 DB 스냅샷을 밀어 넣어(시계 트리거)
 * 보장한다 — 트래픽과 무관하게 ≤5초 신선, 완료 직후 본인 글도 다음 틱에 노출. 쓰기 경로의
 * write-through는 두지 않으므로(캐시에 쓰는 주체 = 스케줄러 하나) dual-write race가 없다.
 *
 * 캐시 MISS는 콜드 스타트(최초 기동/첫 틱 이전)에만 발생하며, 이때 DB 폴백 적재를 Caffeine이
 * single-flight로 묶는다. presigned URL은 캐시에 담지 않고 조회 시점에 생성한다(매 요청 만료되므로).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalFeedService {

    private static final int FEED_SIZE = 20;

    private final UserMissionCompletionRepository completionRepository;
    private final GlobalFeedLocalCache feedCache;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public GlobalFeedResponse getRecentFeed() {
        // 캐시 HIT이면 즉시 반환, 콜드면 DB 폴백 적재(동시 콜드 접근은 single-flight로 1회만 조회).
        List<FeedCacheItem> items = feedCache.getOrLoad(this::loadFromDb);
        return toResponse(items);
    }

    /**
     * DB 스냅샷으로 캐시를 전체 교체한다. (주기 스케줄러에서 호출)
     *
     * 조건부 재캐싱: 피드에 노출되는 완료의 최신 createdAt이 캐시 head(최신값)보다 뒤가 아니면
     * 새 완료가 없다는 뜻이라 상위 20건 재조회·교체를 스킵한다 — 조용한 시간대의 불필요한 전체 조회 제거.
     * 비교는 피드 쿼리와 동일한 "사진 있음" 필터 + createdAt 기준이라 피드 정렬·멤버십과 일치한다.
     * (한계: 피드 안 완료의 삭제는 MAX가 안 변해 못 잡지만, append 위주 최신 20 피드라 실질 영향 없음)
     */
    @Transactional(readOnly = true)
    public void syncCacheFromDb() {
        LocalDateTime dbLatest = completionRepository.findLatestCreatedAtWithImage();
        if (dbLatest == null) {
            return; // 피드에 노출할 완료가 아직 없음 → 기존 캐시 보존
        }
        long dbLatestEpochMilli = dbLatest.toInstant(ZoneOffset.ofHours(9)).toEpochMilli();

        Long cacheLatest = feedCache.peekLatestRecordedAtEpochMilli();
        if (cacheLatest != null && dbLatestEpochMilli <= cacheLatest) {
            return; // 캐시 최신값 이후 새 완료 없음 → 재조회·교체 스킵
        }

        List<FeedCacheItem> items = loadFromDb(); // 새 완료 있음(또는 콜드) → 전체 재조회
        if (items.isEmpty()) {
            return; // 일시적 빈 결과로 기존 피드를 비우지 않음
        }
        feedCache.put(items);
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
