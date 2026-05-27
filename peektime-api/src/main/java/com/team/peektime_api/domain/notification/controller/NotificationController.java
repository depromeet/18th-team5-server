package com.team.peektime_api.domain.notification.controller;

import com.team.peektime_api.domain.notification.dto.PushNotificationRequest;
import com.team.peektime_api.domain.notification.service.FcmService;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "푸시 알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final FcmService fcmService;

    @Operation(summary = "전체 사용자에게 알림 전송")
    @PostMapping("/all")
    public ResponseEntity<SuccessResponse<Void>> sendToAll(
            @RequestParam String title,
            @RequestParam String body) {

        PushNotificationRequest request = PushNotificationRequest.builder()
                .title(title)
                .body(body)
                .build();

        fcmService.sendToAll(request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }
}