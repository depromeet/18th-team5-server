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

        /**
         * SolarTerm 이 없다는건 예외를 터트려야한다. 없으면 절기 정보를 화면에 뿌려 줄수 없으므로
         * 예외를 터트려서 빠르게 인지를 해야한다.
         */
        Optional<SolarTerm> solarTermOpt = solarTermRepository.findByDate(LocalDate.now());
        if (solarTermOpt.isEmpty()) {
            return SeasonalRecordsResponse.of(0, Collections.emptyList());
        }

        // 가독성 개선 필요. Optional 에서 get() 하는 방식은 잘 안쓴다.
        SolarTerm solarTerm = solarTermOpt.get();

        //절기의 시작
        LocalDateTime startDateTime = solarTerm.getStartDate().atStartOfDay();
        //절기의 끝
        LocalDateTime endDateTime = solarTerm.getEndDate().atTime(LocalTime.MAX);

        long recordCount = completionRepository.countByUserIdAndPeriod(
                userId, startDateTime, endDateTime);

        List<RecentRecordCache> recentRecords = getRecentRecordsFromCacheOrDb(userId, startDateTime, endDateTime);

        List<RecentRecord> recentRecordResponses = recentRecords.stream()
                .map(record -> RecentRecord.of(
                        record.completionId(),
                        s3Service.generatePresignedViewUrl(record.objectKey()),
                        record.recordedAt()
                ))
                .toList();

        return SeasonalRecordsResponse.of(recordCount, recentRecordResponses);
    }

    private List<RecentRecordCache> getRecentRecordsFromCacheOrDb(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {

        try {
            List<RecentRecordCache> cached = cacheRepository.findAll(userId);
            if (!cached.isEmpty()) {
                log.info("Redis 캐시 히트: userId={}, count={}", userId, cached.size());
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis 조회 실패, DB fallback: userId={}", userId, e);
        }


        log.info("Redis 캐시 미스, DB 조회: userId={}", userId);
        List<RecentRecordCache> fromDb = completionRepository
                .findRecentRecordsWithImageByPeriod(userId, startDateTime, endDateTime)
                .stream()
                .map(RecentRecordCache::from)
                .toList();

        if (!fromDb.isEmpty()) {
            fromDb.forEach(record -> cacheRepository.addRecord(userId, record));
        }

        return fromDb;
    }
}