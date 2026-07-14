package com.team.peektime_api.domain.mission.controller;

import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.dto.SelectedMissionResponse;
import com.team.peektime_api.domain.mission.dto.SelectedMissionStatusResponse;
import com.team.peektime_api.domain.mission.service.LlmSelectedMissionService;
import com.team.peektime_api.domain.mission.service.SelectedMissionService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mission", description = "미션 관련 API")
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class SelectedMissionController {

    private final SelectedMissionService selectedMissionService;
    private final LlmSelectedMissionService llmSelectedMissionService;

    @Operation(summary = "오늘 선택한 미션 조회", description = "오늘 선택한 미션이 있는지 확인하고, 있으면 미션 정보를 반환합니다.")
    @GetMapping("/selected/today")
    public SuccessResponse<SelectedMissionStatusResponse> getTodaySelectedMission(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(
                SuccessCode.MISSION_FOUND,
                selectedMissionService.getTodaySelectedMission(principal.getUserId())
        );
    }

    @Operation(summary = "선택 미션 조회", description = "필터 조건에 맞는 선택 미션 1개를 반환합니다. 하루에 1번만 선택 가능하며, 이미 선택한 경우 기존 미션을 반환합니다.")
    @PostMapping("/selected")
    public SuccessResponse<SelectedMissionResponse> getSelectedMission(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute SelectedMissionRequest filter
    ) {
        return SuccessResponse.of(
                SuccessCode.MISSION_SELECTED,
                selectedMissionService.getSelectedMission(principal.getUserId(), filter)
        );
    }

    @Operation(summary = "LLM 선택 미션 생성", description = "태그 조건(공간/인원/카테고리)을 기반으로 LLM이 현재 절기에 맞는 미션 1개를 생성해 반환합니다. 생성된 미션은 오늘의 선택 미션으로 배정되며, 기존 선택 미션 완료 API로 수행/기록합니다. 오늘 이미 선택한 미션이 있으면 기존 미션을 반환합니다.")
    @PostMapping("/selected/llm")
    public SuccessResponse<SelectedMissionResponse> generateSelectedMission(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute SelectedMissionRequest filter
    ) {
        return SuccessResponse.of(
                SuccessCode.MISSION_GENERATED,
                llmSelectedMissionService.generateSelectedMission(principal.getUserId(), filter)
        );
    }
}