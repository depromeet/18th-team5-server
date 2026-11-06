package com.team.peektime_api.domain.feed.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전역 피드 좋아요 — "이 사용자가 이 완료 기록에 좋아요를 눌렀다"는 사실 1건.
 *
 * 완료(피드 항목)와의 연관을 엔티티가 아니라 completionId Long으로 들고 있는 이유는
 * {@link com.team.peektime_api.domain.mission.entity.CompletionReaction}과 같다 —
 * 천만 행급 핫 테이블(user_mission_completion)과의 엔티티 결합을 끊는다.
 *
 * (user_id, completion_id) 유니크 제약이 토글의 정합성 백스톱이자 "좋아요 수 = 서로 다른
 * 누른 사람 수"를 보장한다. user_id 선두 복합 인덱스라 "이 20개 중 내가 누른 것"
 * 조회(findLikedCompletionIds)에도 그대로 쓰인다.
 */
@Entity
@Table(name = "feed_like",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_feed_like_user_completion",
                        columnNames = {"user_id", "completion_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "completion_id", nullable = false)
    private Long completionId;

    @Builder(access = AccessLevel.PRIVATE)
    private FeedLike(Long userId, Long completionId) {
        this.userId = userId;
        this.completionId = completionId;
    }

    public static FeedLike of(Long userId, Long completionId) {
        return FeedLike.builder()
                .userId(userId)
                .completionId(completionId)
                .build();
    }
}