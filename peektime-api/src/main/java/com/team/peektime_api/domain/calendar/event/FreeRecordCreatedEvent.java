package com.team.peektime_api.domain.calendar.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 자유 기록 생성 팩트 이벤트
 *
 * - 캐시 무효화에 필요한 userId만 포함
 * - 리스너가 AFTER_COMMIT 시점에 최근 기록 캐시를 삭제
 */
@Getter
@RequiredArgsConstructor
public class FreeRecordCreatedEvent {

    private final Long userId;
    private final Long recordId;

    public static FreeRecordCreatedEvent of(Long userId, Long recordId) {
        return new FreeRecordCreatedEvent(userId, recordId);
    }
}