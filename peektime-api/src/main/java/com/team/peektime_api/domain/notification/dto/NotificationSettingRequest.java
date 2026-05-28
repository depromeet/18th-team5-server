package com.team.peektime_api.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "알림 설정 변경 요청")
@Getter
@Setter
public class NotificationSettingRequest {

    @Schema(description = "오늘의 미션 알림", example = "true")
    private Boolean dailyMission;

    @Schema(description = "절기 마지막 날 알림", example = "true")
    private Boolean solarTermEnd;

    @Schema(description = "절기 변경 알림", example = "true")
    private Boolean solarTermChange;
}
