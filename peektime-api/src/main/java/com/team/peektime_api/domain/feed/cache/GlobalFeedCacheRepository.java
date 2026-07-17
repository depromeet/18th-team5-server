package com.team.peektime_api.domain.feed.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.feed.dto.FeedCacheItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 전역 공유 피드 캐시 (Redis ZSET). 키 1개를 모든 사용자가 공유(전역 단일 피드). score = 완료 시각 → 최신순.
 *
 * <p>TTL 없이 캐시에 상주시킨다(피드는 최대 20건이라 메모리 부담 없음). 만료가 없으므로 읽기 경로에
 * 재적재·분산락(single-flight)이 필요 없고, 캐시와 DB의 불일치(write-through 실패, 삭제 반영 등)는
 * 30초 주기 스케줄러의 {@link #rebuild}(DB 전체 동기화)가 복구한다.
 * <ul>
 *   <li>{@link #addItem} (write-through): 워밍된 캐시에 원자적 ZADD로 보강. 콜드(키 없음)면 추가하지 않아
 *       부분 피드 노출을 막는다 → 스케줄러 동기화가 전체 적재.</li>
 *   <li>{@link #rebuild} (전체 동기화): 임시 키에 적재 후 RENAME으로 원자 교체 → 교체 순간에도 빈 캐시가
 *       노출되지 않고, DB에서 사라진 항목도 캐시에 잔존하지 않는다.</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class GlobalFeedCacheRepository {

    private static final String KEY = "feed:recent:global";
    private static final String REBUILD_KEY = KEY + ":rebuild";
    private static final int MAX_ITEMS = 20;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * write-through: 이미 워밍된(존재하는) 캐시에만 항목을 추가한다.
     * 콜드(키 없음)면 추가하지 않는다 → 항목 1개짜리 부분 피드가 노출되지 않도록, 스케줄러 동기화가 전체 적재.
     */
    public void addItem(FeedCacheItem item) {
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(KEY))) {
                return; // 콜드 → write-through 스킵 (스케줄러 동기화가 전체 적재)
            }
            redisTemplate.opsForZSet().add(KEY, serialize(item), item.recordedAtEpochMilli());
            redisTemplate.opsForZSet().removeRange(KEY, 0, -(MAX_ITEMS + 1));
        } catch (Exception e) {
            log.warn("피드 캐시 write-through 실패: {}", e.getMessage());
        }
    }

    /**
     * DB 조회 결과로 캐시를 전체 교체한다. (30초 주기 스케줄러 + 콜드 캐시 읽기 폴백에서 호출)
     * 기존 키에 병합(ZADD)하면 DB에서 사라진 항목이 잔존하므로, 임시 키에 적재 후 RENAME으로 원자 교체한다.
     * 스냅샷 조회~교체 사이에 write-through된 항목은 덮일 수 있으나 DB에 있으므로 다음 동기화(≤30초)로 복구된다.
     */
    public void rebuild(List<FeedCacheItem> items) {
        if (items.isEmpty()) {
            return; // DB 결과가 비면 교체하지 않음 (일시 오류로 피드를 비우는 것 방지)
        }
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = items.stream()
                    .map(i -> ZSetOperations.TypedTuple.of(serialize(i), (double) i.recordedAtEpochMilli()))
                    .collect(Collectors.toSet());
            redisTemplate.delete(REBUILD_KEY);
            redisTemplate.opsForZSet().add(REBUILD_KEY, tuples);
            redisTemplate.opsForZSet().removeRange(REBUILD_KEY, 0, -(MAX_ITEMS + 1));
            redisTemplate.rename(REBUILD_KEY, KEY);
        } catch (Exception e) {
            log.warn("피드 캐시 재적재 실패: {}", e.getMessage());
        }
    }

    /** 최신순 상위 N개 조회. 실패/미스 시 빈 리스트. */
    public List<FeedCacheItem> findRecent() {
        try {
            Set<String> members = redisTemplate.opsForZSet().reverseRange(KEY, 0, MAX_ITEMS - 1);
            if (members == null || members.isEmpty()) {
                return Collections.emptyList();
            }
            return members.stream()
                    .map(this::deserialize)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("피드 캐시 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String serialize(FeedCacheItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("피드 캐시 직렬화 실패", e);
        }
    }

    private FeedCacheItem deserialize(String json) {
        try {
            return objectMapper.readValue(json, FeedCacheItem.class);
        } catch (JsonProcessingException e) {
            log.error("피드 캐시 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
