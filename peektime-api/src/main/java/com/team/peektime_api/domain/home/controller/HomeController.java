package com.team.peektime_api.domain.home.controller;

import com.team.peektime_api.domain.home.dto.HomeResponse;
import com.team.peektime_api.domain.home.dto.SeasonalRecordsResponse;
import com.team.peektime_api.domain.home.service.HomeCardService;
import com.team.peektime_api.domain.home.service.SeasonalRecordsService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면 API")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeCardService homeCardService;
    private final SeasonalRecordsService seasonalRecordsService;

    @Operation(summary = "홈 메인 카드 조회", description = "현재 절기 정보와 오늘의 미션을 조회합니다.")
    @GetMapping("/card")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<HomeResponse> getHomeCard() {
        return SuccessResponse.of(SuccessCode.HOME_FOUND, homeCardService.getHome());
    }

    @Operation(summary = "절기 기록 조회", description = "현재 절기 내 기록 횟수와 최근 3개의 이미지를 조회합니다.")
    @GetMapping("/seasonal-records")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<SeasonalRecordsResponse> getSeasonalRecords(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(SuccessCode.SEASONAL_RECORDS_FOUND,
                seasonalRecordsService.getSeasonalRecords(principal.getUserId()));
    }
}