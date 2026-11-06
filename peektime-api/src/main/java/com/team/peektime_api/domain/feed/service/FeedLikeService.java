package com.team.peektime_api.domain.feed.service;

import com.team.peektime_api.domain.feed.implement.CompletionLikeCounter;
import com.team.peektime_api.domain.feed.implement.FeedLikeAppender;
import com.team.peektime_api.domain.feed.implement.FeedLikeRemover;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 전역 피드 좋아요 토글 오케스트레이션.
 *
 * 멱등 설계 — 좋아요(POST)는 "눌린 상태를 보장", 취소(DELETE)는 "안 눌린 상태를 보장"이라
 * 같은 요청을 몇 번 보내도 결과가 같다. 클라이언트는 likedByMe에 맞춰 둘 중 하나를 부른다.
 *
 * 의도적으로 @Transactional이 없다: 멤버십 변경(Appender/Remover)과 카운터 UPDATE(Counter)를
 * 각자의 트랜잭션으로 독립 커밋시켜, 동시 중복 INSERT의 제약 위반이 카운터 작업을 오염시키지
 * 않게 한다. 멤버십이 실제로 바뀐 경우에만 카운터를 ±1 하므로 둘은 정합을 유지한다.
 * (트레이드오프: 두 커밋 사이 크래시 시 드문 off-by-1 드리프트 — 진실원본 feed_like의
 * COUNT(*) 재집계로 복구 가능)
 */
@Service
@RequiredArgsConstructor
public class FeedLikeService {

    private final FeedLikeAppender feedLikeAppender;
    private final FeedLikeRemover feedLikeRemover;
    private final CompletionLikeCounter completionLikeCounter;

    public void like(Long userId, Long completionId) {
        if (feedLikeAppender.append(userId, completionId)) {
            completionLikeCounter.increment(completionId);
        }
    }

    public void unlike(Long userId, Long completionId) {
        if (feedLikeRemover.remove(userId, completionId)) {
            completionLikeCounter.decrement(completionId);
        }
    }
}