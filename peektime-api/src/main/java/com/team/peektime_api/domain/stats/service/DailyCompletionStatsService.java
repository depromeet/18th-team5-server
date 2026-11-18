package com.team.peektime_api.domain.stats.service;

import com.team.peektime_api.domain.stats.dto.DailyCompletionStatsResponse;
import com.team.peektime_api.domain.stats.repository.DailyCompletionAggregationRepository;
import com.team.peektime_api.domain.stats.repository.DailyCompletionStatsRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyCompletionStatsService {

    private final DailyCompletionStatsRepository dailyCompletionStatsRepository;
    private final DailyCompletionAggregationRepository dailyCompletionAggregationRepository;

    // 하이브리드 조회 (2단계) — 확정분은 통계 테이블(읽기량이 행 수가 아닌 날짜 수에 비례),
    // 미확정 구간(보통 오늘)만 원본 집계. 분기·병합은 리포지토리의 UNION ALL 쿼리 안에 있다
    @Transactional(readOnly = true)
    public List<DailyCompletionStatsResponse> getDailyStats(LocalDate startDate, LocalDate endDate) {
        return dailyCompletionStatsRepository.findDailyStats(startDate, endDate).stream()
                .map(DailyCompletionStatsResponse::from)
                .toList();
    }

    // 하이브리드와의 성능 비교용 — 통계 테이블을 거치지 않고
    // 전 범위를 원본(user_mission_completion)에서 직접 COUNT 집계
    @Transactional(readOnly = true)
    public List<DailyCompletionStatsResponse> getDailyStatsFromSource(LocalDate startDate, LocalDate endDate) {
        return dailyCompletionAggregationRepository.aggregateDailyStats(startDate, endDate).stream()
                .map(DailyCompletionStatsResponse::from)
                .toList();
    }
}