package com.team.peektime_api.domain.calendar.service;

import com.team.peektime_api.domain.calendar.CalendarCardPolicy;
import com.team.peektime_api.domain.calendar.dto.*;
import com.team.peektime_api.domain.calendar.entity.UserRecord;
import com.team.peektime_api.domain.calendar.repository.UserRecordRepository;
import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.common.enums.MissionType;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.S3.S3Service;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final UserRepository userRepository;
    private final UserRecordRepository userRecordRepository;
    private final UserMissionCompletionRepository completionRepository;
    private final SolarTermRepository solarTermRepository;
    private final S3Service s3Service;

    private static final List<MissionType> CARD_TYPE_ORDER = List.of(
            MissionType.DAILY, MissionType.RECOMMENDED, MissionType.SELECTED
    );

    @Transactional(readOnly = true)
    public CalendarSolarTermResponse getCurrentSolarTermCalendar(Long userId) {
        SolarTerm current = solarTermRepository.findByDate(LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));
        return getSolarTermCalendar(userId, current.getId());
    }

    @Transactional(readOnly = true)
    public CalendarSolarTermResponse getSolarTermCalendar(Long userId, Long solarTermId) {
        List<SolarTerm> solarTerms = solarTermRepository.findTwoStartingFrom(solarTermId);
        if (solarTerms.isEmpty()) {
            throw new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND);
        }

        LocalDate startDate = solarTerms.get(0).getStartDate();
        LocalDate endDate = solarTerms.get(solarTerms.size() - 1).getEndDate();

        List<UserMissionCompletion> completions = completionRepository.findByUserIdAndDateRange(
                userId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        );
        List<UserRecord> records = userRecordRepository.findByUser_IdAndRecordDateBetween(userId, startDate, endDate);

        Map<LocalDate, String> thumbnailByDate = buildThumbnailMap(completions, records);

        List<CalendarSolarTermResponse.SolarTermEntry> entries = solarTerms.stream()
                .map(st -> {
                    List<CalendarSolarTermResponse.DateEntry> dates = st.getStartDate()
                            .datesUntil(st.getEndDate().plusDays(1))
                            .map(date -> new CalendarSolarTermResponse.DateEntry(
                                    date,
                                    generatePresignedUrl(thumbnailByDate.get(date))
                            ))
                            .toList();
                    return new CalendarSolarTermResponse.SolarTermEntry(
                            st.getId(), st.getName(), st.getStartDate(), st.getEndDate(), dates
                    );
                })
                .toList();

        Long lastId = solarTerms.get(solarTerms.size() - 1).getId();
        Long nextSolarTermId = solarTermRepository.findNextAfter(lastId)
                .map(SolarTerm::getId)
                .orElse(null);

        return new CalendarSolarTermResponse(entries, nextSolarTermId);
    }

    @Transactional(readOnly = true)
    public List<CalendarCardResponse> getDayRecords(Long userId, LocalDate date) {
        List<UserMissionCompletion> completions = completionRepository.findByUserIdAndDateRange(
                userId, date.atStartOfDay(), date.atTime(23, 59, 59)
        );
        List<UserRecord> records = userRecordRepository.findByUser_IdAndRecordDate(userId, date);

        List<CalendarCardResponse> cards = new ArrayList<>();

        CARD_TYPE_ORDER.forEach(type ->
                completions.stream()
                        .filter(c -> c.getMissionType() == type)
                        .map(c -> CalendarCardResponse.fromMissionCompletion(c, generatePresignedUrl(c.getObjectKey())))
                        .forEach(cards::add)
        );

        records.stream()
                .map(r -> CalendarCardResponse.fromUserRecord(r, generatePresignedUrl(r.getObjectKey())))
                .forEach(cards::add);

        return cards;
    }

    @Transactional
    public CalendarRecordCreateResponse createFreeRecord(Long userId, CalendarRecordCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate date = request.recordDate();

        long freeCount = userRecordRepository.countByUser_IdAndRecordDate(userId, date);
        if (freeCount >= CalendarCardPolicy.MAX_FREE) {
            throw new BusinessException(ErrorCode.CALENDAR_FREE_RECORD_LIMIT_EXCEEDED);
        }

        long completionCount = completionRepository.findByUserIdAndDateRange(
                userId, date.atStartOfDay(), date.atTime(23, 59, 59)
        ).size();

        if (completionCount + freeCount >= CalendarCardPolicy.MAX_TOTAL) {
            throw new BusinessException(ErrorCode.CALENDAR_TOTAL_CARD_LIMIT_EXCEEDED);
        }

        UserRecord saved = userRecordRepository.save(
                UserRecord.create(user, date, request.objectKey(), request.memo())
        );

        return new CalendarRecordCreateResponse(saved.getId());
    }

    private Map<LocalDate, String> buildThumbnailMap(
            List<UserMissionCompletion> completions, List<UserRecord> records) {

        Map<LocalDate, String> result = new HashMap<>();

        Map<LocalDate, List<UserMissionCompletion>> byDate = completions.stream()
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().toLocalDate()));

        byDate.forEach((date, list) ->
                CARD_TYPE_ORDER.stream()
                        .flatMap(type -> list.stream().filter(c -> c.getMissionType() == type))
                        .filter(c -> c.getObjectKey() != null)
                        .findFirst()
                        .ifPresent(c -> result.put(date, c.getObjectKey()))
        );

        records.stream()
                .filter(r -> r.getObjectKey() != null)
                .filter(r -> !result.containsKey(r.getRecordDate()))
                .forEach(r -> result.put(r.getRecordDate(), r.getObjectKey()));

        return result;
    }

    private String generatePresignedUrl(String objectKey) {
        if (objectKey == null) return null;
        return s3Service.generatePresignedViewUrl(objectKey);
    }
}
