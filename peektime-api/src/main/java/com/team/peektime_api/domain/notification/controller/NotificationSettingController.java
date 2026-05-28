package com.team.peektime_api.domain.notification.controller;

import com.team.peektime_api.domain.notification.dto.NotificationSettingRequest;
import com.team.peektime_api.domain.notification.dto.NotificationSettingResponse;
import com.team.peektime_api.domain.notification.service.NotificationSettingService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification Setting", description = "알림 설정 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @Operation(summary = "알림 설정 조회", description = "현재 사용자의 알림 설정을 조회합니다.")
    @GetMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<NotificationSettingResponse> getSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return SuccessResponse.ok(notificationSettingService.getSettings(principal.getUserId()));
    }

    @Operation(summary = "알림 설정 변경", description = "알림 설정을 변경합니다. 변경할 필드만 보내면 됩니다.")
    @PutMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<NotificationSettingResponse> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody NotificationSettingRequest request) {
        return SuccessResponse.ok(notificationSettingService.updateSettings(principal.getUserId(), request));
    }
}