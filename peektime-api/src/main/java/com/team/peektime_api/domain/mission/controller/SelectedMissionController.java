package com.team.peektime_api.domain.mission.controller;

import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.dto.SelectedMissionResponse;
import com.team.peektime_api.domain.mission.service.SelectedMissionService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Mission", description = "미션 관련 API")
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class SelectedMissionController {

    private final SelectedMissionService selectedMissionService;

    @Operation(summary = "선택 미션 목록 조회", description = "필터 조건에 맞는 선택 미션 목록을 반환합니다. 오늘의 미션과 추천 미션에 포함되지 않은 미션만 조회됩니다.")
    @GetMapping("/selected")
    public SuccessResponse<List<SelectedMissionResponse>> getSelectedMissions(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute SelectedMissionRequest filter
    ) {
        return SuccessResponse.of(
                SuccessCode.MISSION_FOUND,
                selectedMissionService.getSelectedMissions(principal.getUserId(), filter)
        );
    }
}