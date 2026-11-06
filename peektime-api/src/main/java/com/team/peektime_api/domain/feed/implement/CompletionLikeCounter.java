package com.team.peektime_api.domain.feed.implement;

import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비정규화된 좋아요 수(user_mission_completion.like_count) 갱신 전담.
 *
 * 원자 ±1 UPDATE라 핫 완료행의 read-modify-write 유실이 없다. 멤버십(feed_like) 변경이
 * 실제 일어났을 때만 호출되므로 카운터는 "서로 다른 누른 사람 수"에 수렴한다.
 * @Modifying UPDATE는 트랜잭션을 요구하므로 각 메서드가 자기 트랜잭션을 연다.
 */
@Component
@RequiredArgsConstructor
public class CompletionLikeCounter {

    private final UserMissionCompletionRepository completionRepository;

    @Transactional
    public void increment(Long completionId) {
        completionRepository.incrementLikeCount(completionId);
    }

    @Transactional
    public void decrement(Long completionId) {
        completionRepository.decrementLikeCount(completionId);
    }
}