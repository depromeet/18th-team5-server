package com.team.peektime_api.global.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.home.dto.RecentRecordCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RecentRecordsCacheRepository {

    private static final String KEY_PREFIX = "user:";
    private static final String KEY_SUFFIX = ":recent-records";
    private static final int MAX_RECORDS = 3;
    private static final Duration TTL = Duration.ofDays(15);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private String getKey(Long userId) {
        return KEY_PREFIX + userId + KEY_SUFFIX;
    }

    public void addRecord(Long userId, RecentRecordCache record) {
        String key = getKey(userId);
        try {
            String value = objectMapper.writeValueAsString(record);
            redisTemplate.opsForList().leftPush(key, value);
            redisTemplate.opsForList().trim(key, 0, MAX_RECORDS - 1);
            redisTemplate.expire(key, TTL);
            log.debug("Redis 캐시 추가: key={}, record={}", key, record);
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 추가 실패 - JSON 직렬화 오류: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Redis 캐시 추가 실패: {}", e.getMessage());
        }
    }

    public List<RecentRecordCache> findAll(Long userId) {
        String key = getKey(userId);
        try {
            List<String> values = redisTemplate.opsForList().range(key, 0, -1);
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }

            return values.stream()
                    .map(this::deserialize)
                    .filter(record -> record != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void delete(Long userId) {
        String key = getKey(userId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("Redis 캐시 삭제: key={}, deleted={}", key, deleted);
        } catch (Exception e) {
            log.warn("Redis 캐시 삭제 실패: {}", e.getMessage());
        }
    }

    private RecentRecordCache deserialize(String json) {
        try {
            return objectMapper.readValue(json, RecentRecordCache.class);
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}