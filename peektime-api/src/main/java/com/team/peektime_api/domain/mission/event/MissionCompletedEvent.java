package com.team.peektime_api.domain.mission.event;

import com.team.peektime_api.global.common.enums.MissionType;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@RequiredArgsConstructor
public class MissionCompletedEvent {

    private final Long userId;
    private final Long outboxId;
    private final String userUuid;
    private final Long missionId;
    private final MissionType missionType;
    private final Long solarTermId;
    private final LocalDateTime completedAt;

    public static MissionCompletedEvent of(Long userId, OutboxEvent outbox, MissionLogPayload payload) {
        return MissionCompletedEvent.builder()
                .userId(userId)
                .outboxId(outbox.getId())
                .userUuid(payload.userUuid())
                .missionId(payload.missionId())
                .missionType(payload.missionType())
                .solarTermId(payload.solarTermId())
                .completedAt(payload.completedAt())
                .build();
    }
}