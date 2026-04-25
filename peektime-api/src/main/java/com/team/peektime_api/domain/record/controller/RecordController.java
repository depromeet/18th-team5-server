package com.team.peektime_api.domain.record.controller;

import com.team.peektime_api.domain.record.dto.ImageUploadConfirmRequest;
import com.team.peektime_api.domain.record.dto.UserRecordResponse;
import com.team.peektime_api.domain.record.service.RecordService;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Record", description = "기록 관리")
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "이미지 업로드 완료 알림", description = "S3 업로드 완료 후 기록을 저장합니다.")
    @PostMapping("/image-upload")
    public ResponseEntity<SuccessResponse<UserRecordResponse>> confirmImageUpload(
            @RequestBody @Valid ImageUploadConfirmRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.of(SuccessCode.RECORD_CREATED, recordService.confirmImageUpload(request)));
    }
}
