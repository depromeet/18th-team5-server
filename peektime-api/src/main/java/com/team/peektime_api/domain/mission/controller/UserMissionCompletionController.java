package com.team.peektime_api.domain.mission.controller;

import com.team.peektime_api.domain.mission.dto.UserMissionCompletionDetailResponse;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionRequest;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionResponse;
import com.team.peektime_api.domain.mission.service.UserMissionCompletionService;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UserMissionCompletion", description = "미션 완료 기록 관리")
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class UserMissionCompletionController {

    private final UserMissionCompletionService userMissionCompletionService;

    @Operation(summary = "미션 완료 기록", description = "S3 업로드 완료 후 미션 완료를 기록합니다. 동일 미션 중복 완료 시 409 반환.")
    @PostMapping("/{missionId}/complete")
    public ResponseEntity<SuccessResponse<UserMissionCompletionResponse>> completeMission(
            @PathVariable Long missionId,
            @RequestBody @Valid UserMissionCompletionRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.of(SuccessCode.MISSION_COMPLETED, userMissionCompletionService.completeMission(missionId, request)));
    }

    @Operation(summary = "미션 완료 기록 조회", description = "특정 미션의 완료 기록을 조회합니다. 이미지는 Presigned URL로 제공됩니다.")
    @GetMapping("/{missionId}/completions")
    public ResponseEntity<SuccessResponse<List<UserMissionCompletionDetailResponse>>> getMissionCompletions(
            @PathVariable Long missionId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(SuccessResponse.of(SuccessCode.MISSION_FOUND, userMissionCompletionService.getMissionCompletions(missionId, userId)));
    }
}
