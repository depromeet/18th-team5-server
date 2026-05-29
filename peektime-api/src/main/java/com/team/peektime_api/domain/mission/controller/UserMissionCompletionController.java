package com.team.peektime_api.domain.mission.controller;

import com.team.peektime_api.domain.mission.dto.MissionRecordPageResponse;
import com.team.peektime_api.domain.mission.dto.RecommendedMissionCountResponse;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionDetailResponse;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionRequest;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionResponse;
import com.team.peektime_api.domain.mission.service.UserMissionCompletionService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UserMissionCompletion", description = "미션 완료 기록 관리")
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class UserMissionCompletionController {

    private final UserMissionCompletionService userMissionCompletionService;

    @Operation(summary = "오늘의 미션 완료 기록", description = "S3 업로드 완료 후 오늘의 미션 완료를 기록합니다. 동일 미션 중복 완료 시 409 반환.")
    @PostMapping("/{missionId}/complete/daily")
    public SuccessResponse<UserMissionCompletionResponse> completeDailyMission(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long missionId,
            @RequestBody @Valid UserMissionCompletionRequest request
    ) {
        return SuccessResponse.of(SuccessCode.MISSION_COMPLETED,
                userMissionCompletionService.completeDailyMission(principal.getUserId(), missionId, request));
    }



    @Operation(summary = "미션 완료 기록 조회", description = "특정 미션의 완료 기록을 조회합니다. 이미지는 Presigned URL로 제공됩니다.")
    @GetMapping("/{missionId}/completions")
    public SuccessResponse<List<UserMissionCompletionDetailResponse>> getMissionCompletions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long missionId
    ) {
        return SuccessResponse.of(SuccessCode.MISSION_FOUND,
                userMissionCompletionService.getMissionCompletions(principal.getUserId(), missionId));
    }

    @Operation(summary = "미션 기록하기 페이지 정보 조회", description = "미션 기록하기 페이지에 필요한 미션 정보(제목, 설명)를 조회합니다.")
    @GetMapping("/{missionId}/record")
    public SuccessResponse<MissionRecordPageResponse> getMissionRecordPage(
            @PathVariable Long missionId
    ) {
        return SuccessResponse.of(SuccessCode.MISSION_FOUND,
                userMissionCompletionService.getMissionRecordPage(missionId));
    }

    @Operation(summary = "추천 미션 완료 횟수 조회", description = "사용자가 완료한 추천 미션의 총 횟수를 조회합니다.")
    @GetMapping("/recommended/count")
    public SuccessResponse<RecommendedMissionCountResponse> getRecommendedMissionCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(SuccessCode.MISSION_FOUND,
                userMissionCompletionService.getRecommendedMissionCount(principal.getUserId()));
    }
}
