package com.team.peektime_api.domain.feed.implement;

import com.team.peektime_api.domain.feed.entity.FeedLike;
import com.team.peektime_api.domain.feed.repository.FeedLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 좋아요 멤버십 INSERT 전담.
 *
 * 의도적으로 @Transactional을 걸지 않는다 — save()는 리포지토리 자신의 트랜잭션으로
 * 끝나므로, 동시 중복 INSERT가 유니크 제약에 걸려 던지는 예외를 이 자리에서 삼켜도
 * 바깥 작업(좋아요 카운터 UPDATE)을 오염시키지 않는다
 * ({@link com.team.peektime_api.global.infra.llm.LlmDailyCallCounter}와 같은 이유).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedLikeAppender {

    private final FeedLikeRepository feedLikeRepository;

    /**
     * 좋아요를 새로 추가했으면 true, 이미 눌러둔 상태면 false(멱등 무변화).
     * 반환값이 곧 "좋아요 수를 +1 해야 하는가"다.
     */
    public boolean append(Long userId, Long completionId) {
        if (feedLikeRepository.existsByUserIdAndCompletionId(userId, completionId)) {
            return false; // 이미 좋아요 — 멱등
        }
        try {
            feedLikeRepository.save(FeedLike.of(userId, completionId));
            return true;
        } catch (DataIntegrityViolationException e) {
            // exists 체크를 동시에 통과한 요청이 유니크 제약에 걸린 경우 — 이미 눌린 것으로 간주
            log.debug("좋아요 동시 추가 경합 (무해): userId={}, completionId={}", userId, completionId);
            return false;
        }
    }
}