package com.team.peektime_api.domain.mission.event;

import java.time.LocalDate;

public record MissionLogPayload(
        String idempotencyKey,
        Long userId,
        Long solarTermId,
        // 미션을 완료한 날짜. 집계·보상은 로그 "도착 시각"이 아니라 이 값 기준이어야
        // 자정 직전 완료 건이 재시도로 늦게 도착해도 실적 날짜가 어긋나지 않는다
        LocalDate completedDate
) {
    public static MissionLogPayload of(
            String idempotencyKey,
            Long userId,
            Long solarTermId,
            LocalDate completedDate
    ) {
        return new MissionLogPayload(idempotencyKey, userId, solarTermId, completedDate);
    }
}