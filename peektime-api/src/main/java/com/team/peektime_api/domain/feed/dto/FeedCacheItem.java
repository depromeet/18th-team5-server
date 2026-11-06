package com.team.peektime_api.domain.feed.dto;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;

import java.time.ZoneOffset;

/**
 * 전역 피드 로컬 캐시에 저장되는 단위.
 * - presigned URL은 매 요청마다 만료되므로 캐시엔 objectKey만 담고, 조회 시점에 URL을 생성한다.
 * - recordedAtEpochMilli는 완료 시각(최신순 정렬 기준). 목록은 DB 조회 시점에 이미 정렬돼 있다.
 * - likeCount는 비정규화된 좋아요 수(모두에게 동일)라 스냅샷에 그대로 담는다. 5초 주기 재캐싱으로
 *   ≤5초 신선. "내가 눌렀는지"는 사용자별이라 캐시엔 없고 조회 시점에 오버레이한다.
 * - 작성자 정보는 익명 정책에 따라 담지 않는다.
 */
public record FeedCacheItem(
        Long completionId,
        String objectKey,
        long recordedAtEpochMilli,
        int likeCount
) {
    public static FeedCacheItem from(UserMissionCompletion completion) {
        return new FeedCacheItem(
                completion.getId(),
                completion.getObjectKey(),
                completion.getCreatedAt().toInstant(ZoneOffset.ofHours(9)).toEpochMilli(),
                completion.getLikeCount()
        );
    }
}