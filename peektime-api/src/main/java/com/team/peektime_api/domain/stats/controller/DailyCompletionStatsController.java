package com.team.peektime_api.domain.stats.controller;

import com.team.peektime_api.domain.stats.dto.DailyCompletionStatsResponse;
import com.team.peektime_api.domain.stats.service.DailyCompletionStatsService;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Stats", description = "미션 수행 통계")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class DailyCompletionStatsController {

    private final DailyCompletionStatsService dailyCompletionStatsService;

    @Operation(summary = "일별 미션 수행 통계 조회",
            description = "기간 내 일별 미션 완료 수와 사진 포함 완료 수를 조회합니다. "
                    + "확정된 과거는 통계 테이블, 미확정 구간(오늘 등)은 원본 집계를 병합합니다.")
    @GetMapping("/daily-completions")
    public SuccessResponse<List<DailyCompletionStatsResponse>> getDailyCompletionStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return SuccessResponse.of(SuccessCode.DAILY_COMPLETION_STATS_FOUND,
                dailyCompletionStatsService.getDailyStats(startDate, endDate));
    }

    @Operation(summary = "일별 미션 수행 통계 조회 (원본 직접 집계 — 성능 비교용)",
            description = "통계 테이블을 거치지 않고 원본 테이블에서 전 범위를 COUNT로 직접 집계합니다. "
                    + "하이브리드 조회와의 응답 시간 비교 용도이며 결과는 동일해야 합니다.")
    @GetMapping("/daily-completions/raw")
    public SuccessResponse<List<DailyCompletionStatsResponse>> getDailyCompletionStatsFromSource(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return SuccessResponse.of(SuccessCode.DAILY_COMPLETION_STATS_FOUND,
                dailyCompletionStatsService.getDailyStatsFromSource(startDate, endDate));
    }
}