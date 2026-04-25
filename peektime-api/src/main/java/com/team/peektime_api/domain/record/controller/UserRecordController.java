package com.team.peektime_api.domain.record.controller;

import com.team.peektime_api.domain.mission.dto.UserMissionCompletionRequest;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionResponse;
import com.team.peektime_api.domain.record.service.UserRecordService;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "UserRecord", description = "기록 관리")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserRecordController {

    private final UserRecordService userRecordService;

    @Operation(summary = "미션 완료 기록", description = "S3 업로드 완료 후 미션 완료를 기록합니다. 동일 미션 중복 완료 시 409 반환.")
    @PostMapping("/missions/{missionId}/complete")
    public ResponseEntity<SuccessResponse<UserMissionCompletionResponse>> completeMission(
            @PathVariable Long missionId,
            @RequestBody @Valid UserMissionCompletionRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.of(SuccessCode.MISSION_COMPLETED, userRecordService.completeMission(missionId, request)));
    }
}
