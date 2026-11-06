package com.team.peektime_api.domain.feed.implement;

import com.team.peektime_api.domain.feed.repository.FeedLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 멤버십 DELETE 전담.
 *
 * 벌크 DELETE는 트랜잭션을 요구하므로 이 메서드 하나로 자기 트랜잭션을 연다
 * (오케스트레이터는 트랜잭션을 걸지 않아 각 단계가 독립 커밋된다).
 */
@Component
@RequiredArgsConstructor
public class FeedLikeRemover {

    private final FeedLikeRepository feedLikeRepository;

    /**
     * 좋아요를 실제로 지웠으면 true, 애초에 없던 상태면 false(멱등 무변화).
     * 반환값이 곧 "좋아요 수를 -1 해야 하는가"다.
     */
    @Transactional
    public boolean remove(Long userId, Long completionId) {
        return feedLikeRepository.deleteLike(userId, completionId) > 0;
    }
}