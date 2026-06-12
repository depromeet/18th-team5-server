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

@Tag(name = "Notification", description = "푸시 알림 API (내부용)")
@RestController
@RequestMapping("/internal/notifications")
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

    @Operation(summary = "오늘의 미션 알림 전송 (daily_mission 토픽 구독자에게만)")
    @PostMapping("/daily-mission")
    public ResponseEntity<SuccessResponse<Void>> sendDailyMissionNotification(
            @RequestParam String solarTermName) {
        String title = solarTermName + "의 오늘의 미션이 도착했어요!";
        String body = "지금 바로 확인하고 제철을 즐겨보세요 🌿";
        fcmService.sendDailyMissionNotification(title, body);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    @Operation(summary = "절기 마지막 날 알림 전송 (solar_term_end 토픽 구독자에게만)")
    @PostMapping("/solar-term-end")
    public ResponseEntity<SuccessResponse<Void>> sendSolarTermEndNotification(
            @RequestParam String solarTermName) {
        String title = solarTermName + "의 마지막 날이에요";
        String body = "이번 절기에 완료하지 못한 미션이 있다면 지금 확인해보세요! 🍂";
        fcmService.sendSolarTermEndNotification(title, body, solarTermName);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    @Operation(summary = "절기 시작 알림 전송 (solar_term_start 토픽 구독자에게만)")
    @PostMapping("/solar-term-start")
    public ResponseEntity<SuccessResponse<Void>> sendSolarTermStartNotification(
            @RequestParam String solarTermName) {
        String title = "새로운 절기, " + solarTermName + "이 시작되었어요!";
        String body = "이번 절기에는 어떤 이야기가 담겨있을까요? 🌿";
        fcmService.sendSolarTermStartNotification(title, body, solarTermName);
        return ResponseEntity.ok(SuccessResponse.ok());
    }
}