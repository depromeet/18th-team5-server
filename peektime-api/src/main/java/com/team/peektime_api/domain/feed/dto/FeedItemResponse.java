package com.team.peektime_api.domain.feed.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 전역 피드 항목 응답 (익명: 사진 + 시각만 노출, 작성자 비공개).
 * likeCount는 스냅샷에서, likedByMe는 요청 시점 오버레이에서 온다.
 */
public record FeedItemResponse(
        Long feedItemId,
        String photoUrl,
        OffsetDateTime recordedAt,
        int likeCount,
        boolean likedByMe
) {
    public static FeedItemResponse of(FeedCacheItem item, String photoUrl, boolean likedByMe) {
        return new FeedItemResponse(
                item.completionId(),
                photoUrl,
                Instant.ofEpochMilli(item.recordedAtEpochMilli()).atOffset(ZoneOffset.ofHours(9)),
                item.likeCount(),
                likedByMe
        );
    }
}