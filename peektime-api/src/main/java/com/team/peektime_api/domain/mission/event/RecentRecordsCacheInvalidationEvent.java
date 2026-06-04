package com.team.peektime_api.domain.mission.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RecentRecordsCacheInvalidationEvent {

    private final Long userId;

    public static RecentRecordsCacheInvalidationEvent of(Long userId) {
        return new RecentRecordsCacheInvalidationEvent(userId);
    }
}
