package com.team.peektime_api.domain.feed.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.feed.dto.FeedCacheItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 전역 공유 피드 캐시 (Redis ZSET). 키 1개를 모든 사용자가 공유(전역 단일 피드). score = 완료 시각 → 최신순.
 *
 * <p>TTL 정책이 핵심이다. "캐시 워밍 유지"와 "TTL 기반 자가 치유"는 상충한다:
 * write-through가 매 쓰기마다 TTL을 리셋하면 잦은 완료로 키가 영영 만료되지 않아, 한 번 적재에 실패한
 * 사진이 무기한 노출되지 않는다. 그래서 둘을 분리했다.
 * <ul>
 *   <li>{@link #addItem} (write-through): TTL을 <b>리셋하지 않는다</b>. 또 콜드(키 없음)면 추가하지 않아
 *       부분 피드 노출을 막는다 → 워밍된 캐시를 보강하는 역할만 한다.</li>
 *   <li>{@link #rebuild} (재적재): <b>여기서만 TTL을 설정</b>한다 → "재적재 시점부터 60초"라는 예측 가능한
 *       만료 주기를 만들어, 적재 실패분이 최대 60초 내 DB에서 복구되도록 보장한다.</li>
 * </ul>
 * 주기 만료마다 발생하는 동시 재적재(cache-stampede)는 서비스의 분산락(single-flight)으로 차단한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class GlobalFeedCacheRepository {

    private static final String KEY = "feed:recent:global";
    private static final int MAX_ITEMS = 20;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * write-through: 이미 워밍된(존재하는) 캐시에만 항목을 추가한다.
     * - TTL은 건드리지 않는다 → 재적재가 설정한 60초 윈도우가 그대로 흘러 주기적으로 만료된다(자가 치유 보장).
     * - 콜드(키 없음/ TTL 없음)면 추가하지 않는다 → 부분 피드가 노출되지 않도록, 다음 읽기 MISS가 전체 재적재.
     */
    public void addItem(FeedCacheItem item) {
        Long ttl = redisTemplate.getExpire(KEY); // -2: 키 없음, -1: TTL 없음, >0: 남은 TTL(초)
        if (ttl == null || ttl < 0) {
            return; // 콜드 → write-through 스킵 (읽기 MISS가 전체 재적재)
        }
        try {
            redisTemplate.opsForZSet().add(KEY, serialize(item), item.recordedAtEpochMilli());
            redisTemplate.opsForZSet().removeRange(KEY, 0, -(MAX_ITEMS + 1));
            // expire 호출하지 않음 → TTL 리셋 금지 (주기 만료 유지)
        } catch (Exception e) {
            log.warn("피드 캐시 write-through 실패: {}", e.getMessage());
        }
    }

    /**
     * 캐시 MISS 시 DB 결과로 전체 재적재 + TTL 설정. (서비스의 single-flight 안에서만 호출)
     * 여기서만 TTL을 설정해 "재적재 시점부터 60초"라는 예측 가능한 만료 주기를 만든다.
     */
    public void rebuild(List<FeedCacheItem> items) {
        if (items.isEmpty()) {
            return;
        }
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = items.stream()
                    .map(i -> ZSetOperations.TypedTuple.of(serialize(i), (double) i.recordedAtEpochMilli()))
                    .collect(Collectors.toSet());
            redisTemplate.opsForZSet().add(KEY, tuples);
            redisTemplate.opsForZSet().removeRange(KEY, 0, -(MAX_ITEMS + 1));
            redisTemplate.expire(KEY, TTL);
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
