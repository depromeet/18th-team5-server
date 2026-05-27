package com.team.peektime_api.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Schema(description = "푸시 알림 요청")
@Getter
@Builder
public class PushNotificationRequest {

    @Schema(description = "알림 제목")
    private String title;

    @Schema(description = "알림 내용")
    private String body;

    @Schema(description = "추가 데이터")
    private Map<String, String> data;

    @Schema(description = "이미지 URL (선택)")
    private String imageUrl;
}