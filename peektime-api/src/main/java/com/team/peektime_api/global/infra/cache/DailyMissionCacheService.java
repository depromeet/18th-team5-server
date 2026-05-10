package com.team.peektime_api.global.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMissionCacheService {

    private static final String CACHE_KEY_PREFIX = "daily_mission:";
    private static final Duration CACHE_TTL = Duration.ofHours(30);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(LocalDate date, AdminHomeResponse data) {
        String key = generateKey(date);
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            log.info("캐시 저장 완료: key={}", key);
        } catch (JsonProcessingException e) {
            log.error("캐시 저장 실패 - JSON 직렬화 오류: {}", e.getMessage());
            throw new RuntimeException("캐시 저장 실패", e);
        }
    }

    public Optional<AdminHomeResponse> get(LocalDate date) {
        String key = generateKey(date);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            log.debug("캐시 미스: key={}", key);
            return Optional.empty();
        }

        try {
            AdminHomeResponse data = objectMapper.readValue(json, AdminHomeResponse.class);
            log.debug("캐시 히트: key={}", key);
            return Optional.of(data);
        } catch (JsonProcessingException e) {
            log.error("캐시 조회 실패 - JSON 역직렬화 오류: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean exists(LocalDate date) {
        String key = generateKey(date);
        return redisTemplate.hasKey(key);
    }

    private String generateKey(LocalDate date) {
        return CACHE_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}