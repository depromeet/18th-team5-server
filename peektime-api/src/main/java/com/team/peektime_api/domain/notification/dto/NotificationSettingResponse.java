package com.team.peektime_api.domain.notification.dto;

import com.team.peektime_api.domain.notification.entity.NotificationSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "알림 설정 응답")
@Getter
@Builder
public class NotificationSettingResponse {

    @Schema(description = "오늘의 미션 알림", example = "true")
    private Boolean dailyMission;

    @Schema(description = "절기 마지막 날 알림", example = "true")
    private Boolean solarTermEnd;

    @Schema(description = "절기 시작 알림", example = "true")
    private Boolean solarTermStart;

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return NotificationSettingResponse.builder()
                .dailyMission(setting.getDailyMission())
                .solarTermEnd(setting.getSolarTermEnd())
                .solarTermStart(setting.getSolarTermStart())
                .build();
    }
}