package com.team.peektime_api.domain.feed.repository;

import com.team.peektime_api.domain.feed.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    boolean existsByUserIdAndCompletionId(Long userId, Long completionId);

    // 벌크 DELETE — 영향받은 행 수를 그대로 돌려줘(1 또는 0) 실제 좋아요 취소가 일어났는지
    // 판별한다. 엔티티를 로드하지 않아 핫 경로에 가볍다
    @Modifying
    @Query("DELETE FROM FeedLike fl WHERE fl.userId = :userId AND fl.completionId = :completionId")
    int deleteLike(@Param("userId") Long userId, @Param("completionId") Long completionId);

    // 피드 20건 중 이 사용자가 누른 completionId만 — likedByMe 오버레이용.
    // 전역 단일 키 캐시엔 담을 수 없는 사용자별 상태라 요청 시점에 한 방 IN 쿼리로 덧씌운다
    @Query("SELECT fl.completionId FROM FeedLike fl " +
            "WHERE fl.userId = :userId AND fl.completionId IN :completionIds")
    List<Long> findLikedCompletionIds(@Param("userId") Long userId,
                                      @Param("completionIds") List<Long> completionIds);
}