package com.team.peektime_api.domain.calendar.controller;

import com.team.peektime_api.domain.calendar.dto.*;
import com.team.peektime_api.domain.calendar.service.CalendarService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Calendar", description = "캘린더 기록 관리")
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(
            summary = "현재 절기 캘린더 조회",
            description = "캘린더 최초 진입 시 사용합니다. 오늘 날짜 기준으로 현재 절기를 자동 판단하여 2개 절기의 날짜별 기록 썸네일을 반환합니다."
    )
    @GetMapping("/solar-terms")
    public SuccessResponse<CalendarSolarTermResponse> getCurrentSolarTermCalendar(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(SuccessCode.CALENDAR_SOLAR_TERM_FOUND,
                calendarService.getCurrentSolarTermCalendar(principal.getUserId()));
    }

    @Operation(
            summary = "절기 캘린더 조회 (페이지네이션)",
            description = "이전/다음 절기로 이동할 때 사용합니다. 이전 응답의 nextSolarTermId를 전달하면 해당 절기부터 2개 절기의 날짜별 기록 썸네일을 반환합니다. nextSolarTermId가 null이면 마지막 페이지입니다."
    )
    @GetMapping("/solar-terms/{solarTermId}")
    public SuccessResponse<CalendarSolarTermResponse> getSolarTermCalendar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long solarTermId
    ) {
        return SuccessResponse.of(SuccessCode.CALENDAR_SOLAR_TERM_FOUND,
                calendarService.getSolarTermCalendar(principal.getUserId(), solarTermId));
    }

    @Operation(summary = "날짜별 기록 조회", description = "특정 날짜의 모든 기록 카드를 반환합니다. date는 yyyy-MM-dd 형식으로 전달합니다. (예: 2026-05-26) 최대 5개이며 DAILY > RECOMMENDED > SELECTED > FREE 순으로 정렬됩니다.")
    @GetMapping("/records/{date}")
    public SuccessResponse<List<CalendarCardResponse>> getDayRecords(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return SuccessResponse.of(SuccessCode.CALENDAR_DAY_FOUND,
                calendarService.getDayRecords(principal.getUserId(), date));
    }

    @Operation(summary = "자유 기록 추가", description = "특정 날짜에 자유 기록을 추가합니다. 하루 1개 제한, 전체 5개 초과 시 409 반환.")
    @PostMapping("/records")
    public SuccessResponse<CalendarRecordCreateResponse> createFreeRecord(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CalendarRecordCreateRequest request
    ) {
        return SuccessResponse.of(SuccessCode.CALENDAR_RECORD_CREATED,
                calendarService.createFreeRecord(principal.getUserId(), request));
    }
}
