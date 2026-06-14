package com.team.peektime_api.global.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PresignedUrlCacheRepository {

    private static final String KEY_PREFIX = "presigned:";
    private static final Duration TTL = Duration.ofDays(6);

    private final RedisTemplate<String, String> redisTemplate;

    private String getKey(String objectKey) {
        return KEY_PREFIX + objectKey;
    }

    public Optional<String> get(String objectKey) {
        try {
            String url = redisTemplate.opsForValue().get(getKey(objectKey));
            return Optional.ofNullable(url);
        } catch (Exception e) {
            log.warn("presigned URL 캐시 조회 실패: objectKey={}", objectKey, e);
            return Optional.empty();
        }
    }

    public void put(String objectKey, String presignedUrl) {
        try {
            redisTemplate.opsForValue().set(getKey(objectKey), presignedUrl, TTL);
        } catch (Exception e) {
            log.warn("presigned URL 캐시 저장 실패: objectKey={}", objectKey, e);
        }
    }
}
