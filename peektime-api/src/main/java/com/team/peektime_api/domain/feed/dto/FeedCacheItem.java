package com.team.peektime_api.domain.feed.dto;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;

import java.time.ZoneOffset;

/**
 * 전역 피드 캐시(Redis ZSET)에 저장되는 단위.
 * - presigned URL은 매 요청마다 만료되므로 캐시엔 objectKey만 담고, 조회 시점에 URL을 생성한다.
 * - recordedAtEpochMilli는 ZSET score로도 쓰여 최신순 정렬의 기준이 된다.
 * - 작성자 정보는 익명 정책에 따라 담지 않는다.
 */
public record FeedCacheItem(
        Long completionId,
        String objectKey,
        long recordedAtEpochMilli
) {
    public static FeedCacheItem from(UserMissionCompletion completion) {
        return new FeedCacheItem(
                completion.getId(),
                completion.getObjectKey(),
                completion.getCreatedAt().toInstant(ZoneOffset.ofHours(9)).toEpochMilli()
        );
    }
}