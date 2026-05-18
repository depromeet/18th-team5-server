package com.team.peektime_api.domain.home.service;

import com.team.peektime_api.domain.home.dto.RecentRecordCache;
import com.team.peektime_api.domain.home.dto.SeasonalRecordsResponse;
import com.team.peektime_api.domain.home.dto.SeasonalRecordsResponse.RecentRecord;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.global.infra.S3.S3Service;
import com.team.peektime_api.global.infra.cache.RecentRecordsCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonalRecordsService {

    private final RecentRecordsCacheRepository cacheRepository;
    private final UserMissionCompletionRepository completionRepository;
    private final S3Service s3Service;
    private final SolarTermRepository solarTermRepository;

    public SeasonalRecordsResponse getSeasonalRecords(Long userId) {
        Optional<SolarTerm> solarTermOpt = solarTermRepository.findByDate(LocalDate.now());

        if (solarTermOpt.isEmpty()) {
            return SeasonalRecordsResponse.of(0, Collections.emptyList());
        }

        SolarTerm solarTerm = solarTermOpt.get();
        LocalDateTime startDateTime = solarTerm.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = solarTerm.getEndDate().atTime(LocalTime.MAX);

        long recordCount = completionRepository.countByUser_IdAndCompletedAtBetween(
                userId, startDateTime, endDateTime);

        List<RecentRecordCache> recentRecords = getRecentRecordsFromCacheOrDb(userId, startDateTime, endDateTime);

        List<RecentRecord> recentRecordResponses = recentRecords.stream()
                .map(record -> RecentRecord.of(
                        record.completionId(),
                        s3Service.generatePresignedViewUrl(record.objectKey())
                ))
                .toList();

        return SeasonalRecordsResponse.of(recordCount, recentRecordResponses);
    }

    private List<RecentRecordCache> getRecentRecordsFromCacheOrDb(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        try {
            List<RecentRecordCache> cached = cacheRepository.findAll(userId);
            if (!cached.isEmpty()) {
                log.debug("Redis 캐시 히트: userId={}", userId);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis 조회 실패, DB fallback: userId={}", userId, e);
        }

        log.debug("Redis 캐시 미스, DB 조회: userId={}", userId);
        List<RecentRecordCache> fromDb = completionRepository
                .findTop3ByUser_IdAndCompletedAtBetweenAndObjectKeyIsNotNullOrderByCompletedAtDesc(
                        userId, startDateTime, endDateTime)
                .stream()
                .map(RecentRecordCache::from)
                .toList();

        if (!fromDb.isEmpty()) {
            fromDb.forEach(record -> cacheRepository.addRecord(userId, record));
        }

        return fromDb;
    }
}