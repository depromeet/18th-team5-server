package com.team.peektime_api.domain.feed.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 전역 피드 항목 응답 (익명: 사진 + 시각만 노출, 작성자 비공개).
 */
public record FeedItemResponse(
        Long feedItemId,
        String photoUrl,
        OffsetDateTime recordedAt
) {
    public static FeedItemResponse of(FeedCacheItem item, String photoUrl) {
        return new FeedItemResponse(
                item.completionId(),
                photoUrl,
                Instant.ofEpochMilli(item.recordedAtEpochMilli()).atOffset(ZoneOffset.ofHours(9))
        );
    }
}