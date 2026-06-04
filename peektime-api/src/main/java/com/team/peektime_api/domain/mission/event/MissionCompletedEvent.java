package com.team.peektime_api.domain.mission.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 미션 완료 팩트 이벤트
 *
 * - 최소한의 ID만 포함 (Pull 방식)
 * - 리스너가 필요한 데이터를 직접 조회
 */
@Getter
@RequiredArgsConstructor
public class MissionCompletedEvent {

    private final Long completionId;
    private final Long outboxId;

    public static MissionCompletedEvent of(Long completionId, Long outboxId) {
        return new MissionCompletedEvent(completionId, outboxId);
    }
}
