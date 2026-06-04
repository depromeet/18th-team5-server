package com.team.peektime_api.domain.mission.controller;

import com.team.peektime_api.domain.mission.dto.RecommendedMissionAvailabilityResponse;
import com.team.peektime_api.domain.mission.dto.RecommendedMissionResponse;
import com.team.peektime_api.domain.mission.service.RecommendedMissionService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mission", description = "미션 관련 API")
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class RecommendedMissionController {

    private final RecommendedMissionService recommendedMissionService;

    @Operation(summary = "추천 미션 목록 조회", description = "사용자 타입과 EnjoyType 우선순위에 따라 정렬된 추천 미션 15개를 반환합니다.")
    @GetMapping("/recommended")
    public SuccessResponse<RecommendedMissionResponse> getRecommendedMissions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(
                SuccessCode.RECOMMENDED_MISSION_FOUND,
                recommendedMissionService.getRecommendedMissions(principal.getUserId())
        );
    }

    @Operation(summary = "추천 미션 완료 가능 여부 조회", description = "오늘 추천 미션을 추가로 완료할 수 있는지 확인합니다. 하루 최대 3회까지 가능합니다.")
    @GetMapping("/recommended/availability")
    public SuccessResponse<RecommendedMissionAvailabilityResponse> getRecommendedMissionAvailability(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(
                SuccessCode.RECOMMENDED_MISSION_AVAILABILITY_FOUND,
                recommendedMissionService.getAvailability(principal.getUserId())
        );
    }
}
